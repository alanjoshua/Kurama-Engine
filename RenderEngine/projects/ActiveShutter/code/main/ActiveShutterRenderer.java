package main;

import Kurama.Math.Matrix;
import Kurama.Math.Vector;
import Kurama.Vulkan.*;
import Kurama.game.Game;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.function.Consumer;

import static Kurama.Vulkan.RenderUtils.compactDraws;
import static Kurama.Vulkan.VulkanUtilities.*;
import static Kurama.utils.Logger.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.KHRDynamicRendering.*;
import static org.lwjgl.vulkan.KHRMultiview.VK_KHR_MULTIVIEW_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.KHRSynchronization2.VK_IMAGE_LAYOUT_ATTACHMENT_OPTIMAL_KHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.VK_STRUCTURE_TYPE_RENDER_PASS_MULTIVIEW_CREATE_INFO;
import static org.lwjgl.vulkan.VK12.*;

public class ActiveShutterRenderer extends VulkanRendererBase {

    public int MAXOBJECTS = 10000;
    public static final int MAX_FRAMES_IN_FLIGHT = 1;
    public AllocatedBuffer gpuSceneBuffer;
    public List<Long> imageInputDescriptorSets;

    public static class MultiViewRenderPass {
        public long frameBuffer;
        public List<MultiViewRenderPassFrame> frames;
        public FrameBufferAttachment depthAttachment;
        public FrameBufferAttachment colorAttachment;
        public long renderPass = VK_NULL_HANDLE;
        // Global Descriptor set contains the camera data and other scene parameters
        public long cameraAndSceneDescriptorSetLayout;
        public long computeDescriptorSetLayout;
        // This contains the object transformation matrices
        public long objectDescriptorSetLayout;
    }
    public MultiViewRenderPass multiViewRenderPass = new MultiViewRenderPass();
    public int currentMultiViewFrameIndex = 0;
    public long multiViewPipelineLayout;
    public long multiViewGraphicsPipeline;
    public long computePipelineLayout;
    public long computePipeline;
    public long textureSetLayout;
    public boolean windowResized;
    public GPUCameraData gpuCameraDataLeft;
    public GPUCameraData gpuCameraDataRight;
    public GPUSceneData gpuSceneData;
    public ComputeUBOIn computeUBOIn = new ComputeUBOIn();
    public int multiViewNumLayers = 2;
    public boolean shouldUpdateGPUSceneBuffer = true;
    public int objectRenderCount;

    public ActiveShutterRenderer(Game game) {
        super(game);
        DEVICE_EXTENSIONS.add(VK_KHR_MULTIVIEW_EXTENSION_NAME);
    }

    public void render() {
        try (var stack = stackPush()) {

            var curMultiViewFrame = multiViewRenderPass.frames.get(currentMultiViewFrameIndex);

            // CPU blocks for a value of 2, which indicated that it is safe to run cull compute again
            var waitSemaphoreInfo = VkSemaphoreWaitInfo.calloc(stack);
            waitSemaphoreInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_WAIT_INFO);
            waitSemaphoreInfo.pSemaphores(stack.longs(curMultiViewFrame.timeLineSemaphore));
            waitSemaphoreInfo.pValues(stack.longs(2));
            waitSemaphoreInfo.semaphoreCount(1);
            vkWaitSemaphores(device, waitSemaphoreInfo, UINT64_MAX);

            performBufferDataUpdates(renderables, currentMultiViewFrameIndex);

            curMultiViewFrame.timeLineSemaphore = resetTimelineSemaphore(curMultiViewFrame.timeLineSemaphore);

            // Recreate swap chain items if the window is resized
            // Do it before running the pipeline
            if(windowResized) {
                recreateSwapChain();
                windowResized = false;
            }

            callCompute(curMultiViewFrame, stack);
            renderMultiViewFrame(curMultiViewFrame, stack);

            prepareFrame();
            drawViewFrame(curMultiViewFrame.timeLineSemaphore, 0);
            submitFrame();

            prepareFrame();
            drawViewFrame(curMultiViewFrame.timeLineSemaphore, 1);
            submitFrame();

            var bufferReader = new BufferWriter(vmaAllocator, curMultiViewFrame.indirectDrawCountBuffer, Integer.BYTES, Integer.BYTES);
            bufferReader.mapBuffer();

            objectRenderCount = bufferReader.buffer.getInt();

            bufferReader.unmapBuffer();

            logPerSec("Rendered object count: " + objectRenderCount);

            // Only for multiview
            currentMultiViewFrameIndex = (currentMultiViewFrameIndex + 1) % MAX_FRAMES_IN_FLIGHT;
        }
    }

    public void tick() {

    }

    // Update frustum whenever camera is updated
    public void cameraUpdatedEvent() {
        for(var frame: multiViewRenderPass.frames) {
            frame.shouldUpdateCameraBuffer = true;
            frame.shouldUpdateComputeUboBuffer = true;
        }
        computeUBOIn.frustumPlanes = Arrays.asList(((ActiveShutterGame)game).mainCamera.frustumIntersection.planes);
    }

    public void geometryUpdatedEvent() {
        computeUBOIn.objectCount = renderables.size();
        recordComputeCommandBuffers();
        int i = 0;
        for(var frame: multiViewRenderPass.frames) {
                recordMultiViewCommandBuffer(renderables, frame, i);
                frame.renderablesUpdated = true;
                frame.shouldUpdateObjectBuffer = true;
                frame.shouldUpdateIndirectCommandsBuffer = true;
                i++;
        }
    }

    public void initRenderer() {

        multiViewRenderPass.frames = new ArrayList<>(MAX_FRAMES_IN_FLIGHT);
        for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
            var newFrame = new MultiViewRenderPassFrame();
            newFrame.vmaAllocator = vmaAllocator;
            multiViewRenderPass.frames.add(newFrame);
        }

        initComputeCmdPoolsCmdBuffersSyncObjects();
        initMultiViewCmdPoolsAndBuffers();
        initMultiViewSyncObjects();

        createMultiViewColorAttachment();
        createMultiViewDepthAttachment();

        initMultiViewFrameBuffers();

        initBuffers();
        initDescriptorSets(); // Descriptor set layout is needed when both defining the pipelines

        deletionQueue.add(() -> vmaDestroyBuffer(vmaAllocator, gpuSceneBuffer.buffer, gpuSceneBuffer.allocation));
        deletionQueue.add(() -> multiViewRenderPass.frames.forEach(MultiViewRenderPassFrame::cleanUp));

        initPipelines();
    }

    public void initBuffers() {

        var objectBufferSize = (int)(padUniformBufferSize(GPUObjectData.SIZEOF, minUniformBufferOffsetAlignment)) * MAXOBJECTS;
        objectBufferSize = GPUObjectData.SIZEOF * MAXOBJECTS;
        var cameraBufferSize = GPUCameraData.SIZEOF * multiViewNumLayers;

        var sceneParamsBufferSize = MAX_FRAMES_IN_FLIGHT * padUniformBufferSize(GPUSceneData.SIZEOF, minUniformBufferOffsetAlignment);

        gpuSceneBuffer = createBufferVMA(vmaAllocator,
                sceneParamsBufferSize,
                VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                VMA_MEMORY_USAGE_AUTO,
                VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT |
                        VMA_ALLOCATION_CREATE_HOST_ACCESS_ALLOW_TRANSFER_INSTEAD_BIT
        );

        for(var frame: multiViewRenderPass.frames) {

            frame.indirectCommandBuffer = createBufferVMA(
                    vmaAllocator,
                    MAXOBJECTS * VkDrawIndexedIndirectCommand.SIZEOF,
                    VK_BUFFER_USAGE_STORAGE_BUFFER_BIT |  VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT,
                    VMA_MEMORY_USAGE_AUTO,
                    VMA_ALLOCATION_CREATE_DEDICATED_MEMORY_BIT
            );

            frame.indirectDrawCountBuffer = createBufferVMA(
                    vmaAllocator,
                    Integer.BYTES,
                    VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
                    VMA_MEMORY_USAGE_AUTO,
                    VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT |
                            VMA_ALLOCATION_CREATE_HOST_ACCESS_ALLOW_TRANSFER_INSTEAD_BIT
            );

            frame.computeUBOBuffer = createBufferVMA(
                    vmaAllocator,
                    ComputeUBOIn.SIZEOF,
                    VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                    VMA_MEMORY_USAGE_AUTO,
                    VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT |
                            VMA_ALLOCATION_CREATE_HOST_ACCESS_ALLOW_TRANSFER_INSTEAD_BIT
            );

            // uniform buffer for GPU camera data
            // A camera buffer is created for each frame
            frame.cameraBuffer
                    = createBufferVMA(
                    vmaAllocator,
                    cameraBufferSize,
                    VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                    VMA_MEMORY_USAGE_AUTO,
                    VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT |
                            VMA_ALLOCATION_CREATE_HOST_ACCESS_ALLOW_TRANSFER_INSTEAD_BIT);

            // Object buffer
            frame.objectBuffer =
                    createBufferVMA(
                            vmaAllocator,
                            objectBufferSize,
                            VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
                            VMA_MEMORY_USAGE_AUTO,
                            VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT |
                                    VMA_ALLOCATION_CREATE_HOST_ACCESS_ALLOW_TRANSFER_INSTEAD_BIT);

        }
    }

    public void recordViewCommandBuffer(int viewImageToRender) {

        var commandBuffer = drawCmds.get(currentDisplayBufferIndex);

        try (var stack = stackPush()) {
            if(vkResetCommandBuffer(commandBuffer, 0) != VK_SUCCESS) {
                throw new RuntimeException("Failed to begin recording command buffer");
            }

            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

            if (vkBeginCommandBuffer(commandBuffer, beginInfo) != VK_SUCCESS) {
                throw new RuntimeException("Failed to begin recording command buffer");
            }

            insertImageMemoryBarrier(commandBuffer, swapChainAttachments.get(currentDisplayBufferIndex).swapChainImage,
                    0, VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT,
                    VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                    VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
                    VkImageSubresourceRange.calloc(stack)
                            .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                            .baseMipLevel(0)
                            .levelCount(1)
                            .layerCount(1)
                            .baseArrayLayer(0));

            var colorAttachment = VkRenderingAttachmentInfoKHR.calloc(1, stack);
            colorAttachment.sType(VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO_KHR);
            colorAttachment.imageView(swapChainAttachments.get(currentDisplayBufferIndex).swapChainImageView);
            colorAttachment.imageLayout(VK_IMAGE_LAYOUT_ATTACHMENT_OPTIMAL_KHR);
            colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
            colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
            colorAttachment.clearValue().color(VkClearValue.calloc(stack).color().float32(stack.floats(0.0f, 0.0f, 0.0f, 0.0f)));

            var depthStencilAttachment = VkRenderingAttachmentInfoKHR.calloc(1, stack);
            depthStencilAttachment.sType(VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO_KHR);
            depthStencilAttachment.imageView(VK_NULL_HANDLE);

            VkRect2D renderArea = VkRect2D.calloc(stack);
            renderArea.offset(VkOffset2D.calloc(stack).set(0, 0));
            renderArea.extent(swapChainExtent);

            var renderingInfo = VkRenderingInfo.calloc(stack);
            renderingInfo.sType(VK_STRUCTURE_TYPE_RENDERING_INFO_KHR);
            renderingInfo.renderArea(renderArea);
            renderingInfo.layerCount(1);
            renderingInfo.pColorAttachments(colorAttachment);
            renderingInfo.pDepthAttachment(depthStencilAttachment.get(0));
            renderingInfo.pStencilAttachment(depthStencilAttachment.get(0));

            vkCmdBeginRenderingKHR(commandBuffer, renderingInfo);

            var viewportBuffer = VkViewport.calloc(1, stack);
            viewportBuffer.width(swapChainExtent.width());
            viewportBuffer.height(swapChainExtent.height());
            viewportBuffer.minDepth(0.0f);
            viewportBuffer.maxDepth(1.0f);

            var scissorBuffer = VkRect2D.calloc(1, stack);
            scissorBuffer.offset(VkOffset2D.calloc(stack).set(0, 0));
            scissorBuffer.extent(swapChainExtent);

            vkCmdSetViewport(commandBuffer, 0, viewportBuffer);
            vkCmdSetScissor(commandBuffer, 0, scissorBuffer);

            {
                vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline);

                // Bind color Attachment from previous multiview Renderpass as input
                vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                        pipelineLayout, 0,
                        stack.longs(imageInputDescriptorSets.get(currentDisplayBufferIndex)), null);

                vkCmdPushConstants(commandBuffer,
                        pipelineLayout,
                        VK_SHADER_STAGE_FRAGMENT_BIT,
                        0,
                        stack.floats(viewImageToRender));

                // Render a fullscreen triangle, so that the fragment shader is run for each pixel
                vkCmdDraw(commandBuffer, 3, 1,0, 0);
            }

            vkCmdEndRenderingKHR(commandBuffer);

            insertImageMemoryBarrier(commandBuffer, swapChainAttachments.get(currentDisplayBufferIndex).swapChainImage,
                    VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT, 0,
                    VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, VK_IMAGE_LAYOUT_PRESENT_SRC_KHR,
                    VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
                    VkImageSubresourceRange.calloc(stack)
                            .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                            .baseMipLevel(0)
                            .levelCount(1)
                            .layerCount(1)
                            .baseArrayLayer(0));

            if (vkEndCommandBuffer(commandBuffer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to record command buffer");
            }

        }

    }

    private void recordComputeCommandBuffers() {
        try (var stack = stackPush()) {

            for(int i = 0; i < multiViewRenderPass.frames.size(); i++) {
                var frame = multiViewRenderPass.frames.get(i);
                var commandBuffer = frame.computeCommandBuffer;

                VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
                beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

                vkCheck(vkBeginCommandBuffer(commandBuffer, beginInfo), "Could not begin compute command buffer");

                // Acquire barrier
                // Add memory barrier to ensure that the indirect commands have been consumed before the compute shader updates them
                if(queueFamilyIndices.graphicsFamily != queueFamilyIndices.computeFamily) {

                    var bufferBarrier = VkBufferMemoryBarrier.calloc(1, stack);
                    bufferBarrier.sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER);
                    bufferBarrier.srcAccessMask(0);
                    bufferBarrier.dstAccessMask(VK_ACCESS_SHADER_WRITE_BIT);
                    bufferBarrier.srcQueueFamilyIndex(queueFamilyIndices.graphicsFamily);
                    bufferBarrier.dstQueueFamilyIndex(queueFamilyIndices.computeFamily);
                    bufferBarrier.buffer(frame.indirectCommandBuffer.buffer);
                    bufferBarrier.offset(0);
                    bufferBarrier.size(VK_WHOLE_SIZE);

                    vkCmdPipelineBarrier(commandBuffer,
                            VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                            0, null, bufferBarrier, null);
                }

                vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, computePipeline);
                vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, computePipelineLayout, 0, stack.longs(frame.computeDescriptorSet), null);

                vkCmdDispatch(commandBuffer, (computeUBOIn.objectCount / 16)+1, 1, 1);

                // Release barrier
                if(queueFamilyIndices.graphicsFamily != queueFamilyIndices.computeFamily) {
                    var bufferBarrier = VkBufferMemoryBarrier.calloc(1, stack);
                    bufferBarrier.sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER);
                    bufferBarrier.srcAccessMask(VK_ACCESS_SHADER_WRITE_BIT);
                    bufferBarrier.dstAccessMask(0);
                    bufferBarrier.srcQueueFamilyIndex(queueFamilyIndices.computeFamily);
                    bufferBarrier.dstQueueFamilyIndex(queueFamilyIndices.graphicsFamily);
                    bufferBarrier.buffer(frame.indirectCommandBuffer.buffer);
                    bufferBarrier.offset(0);
                    bufferBarrier.size(VK_WHOLE_SIZE);

                    vkCmdPipelineBarrier(commandBuffer,
                            VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT,
                            0, null, bufferBarrier, null);
                }

                vkEndCommandBuffer(commandBuffer);
            }

        }
    }

    public void recordMultiViewCommandBuffer(List<Renderable> renderables, MultiViewRenderPassFrame currentFrame, int frameIndex) {

        var commandBuffer = currentFrame.commandBuffer;

        try (var stack = stackPush()) {

            if(vkResetCommandBuffer(commandBuffer, 0) != VK_SUCCESS) {
                throw new RuntimeException("Failed to begin recording command buffer");
            }

            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

            VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc(stack);
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO);

            renderPassInfo.renderPass(multiViewRenderPass.renderPass);

            VkRect2D renderArea = VkRect2D.calloc(stack);
            renderArea.offset(VkOffset2D.calloc(stack).set(0, 0));
            renderArea.extent(swapChainExtent);
            renderPassInfo.renderArea(renderArea);

            VkClearValue.Buffer clearValues = VkClearValue.calloc(2, stack);
            clearValues.get(0).color().float32(stack.floats(0.0f, 0.0f, 0.0f, 1.0f));
            clearValues.get(1).depthStencil().set(1.0f, 0);

            renderPassInfo.pClearValues(clearValues);

            if (vkBeginCommandBuffer(commandBuffer, beginInfo) != VK_SUCCESS) {
                throw new RuntimeException("Failed to begin recording command buffer");
            }

            var viewportBuffer = VkViewport.calloc(1, stack);
            viewportBuffer.width(swapChainExtent.width());
            viewportBuffer.height(swapChainExtent.height());
            viewportBuffer.minDepth(0);
            viewportBuffer.maxDepth(1);

            var scissorBuffer = VkRect2D.calloc(1, stack);
            scissorBuffer.offset(VkOffset2D.calloc(stack).set(0, 0));
            scissorBuffer.extent(swapChainExtent);

            vkCmdSetViewport(commandBuffer, 0, viewportBuffer);
            vkCmdSetScissor(commandBuffer, 0, scissorBuffer);

            // Acquire barrier
            // Add memory barrier to ensure that the indirect commands have been consumed before the compute shader updates them
            if(queueFamilyIndices.graphicsFamily != queueFamilyIndices.computeFamily) {

                var bufferBarrier = VkBufferMemoryBarrier.calloc(1, stack);
                bufferBarrier.sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER);
                bufferBarrier.srcAccessMask(0);
                bufferBarrier.dstAccessMask(VK_ACCESS_INDIRECT_COMMAND_READ_BIT);
                bufferBarrier.srcQueueFamilyIndex(queueFamilyIndices.computeFamily);
                bufferBarrier.dstQueueFamilyIndex(queueFamilyIndices.graphicsFamily);
                bufferBarrier.buffer(currentFrame.indirectCommandBuffer.buffer);
                bufferBarrier.offset(0);
                bufferBarrier.size(VK_WHOLE_SIZE);

                vkCmdPipelineBarrier(commandBuffer,
                        VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, VK_PIPELINE_STAGE_DRAW_INDIRECT_BIT,
                        0, null, bufferBarrier, null);
            }

            renderPassInfo.framebuffer(multiViewRenderPass.frameBuffer);

            vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);
            {
                recordSceneToCommandBufferIndirect(renderables, commandBuffer, currentFrame, frameIndex, stack);
            }
            vkCmdEndRenderPass(commandBuffer);

            // Release barrier
            if(queueFamilyIndices.graphicsFamily != queueFamilyIndices.computeFamily) {

                var bufferBarrier = VkBufferMemoryBarrier.calloc(1, stack);
                bufferBarrier.sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER);
                bufferBarrier.srcAccessMask(VK_ACCESS_INDIRECT_COMMAND_READ_BIT);
                bufferBarrier.dstAccessMask(0);
                bufferBarrier.srcQueueFamilyIndex(queueFamilyIndices.graphicsFamily);
                bufferBarrier.dstQueueFamilyIndex(queueFamilyIndices.computeFamily);
                bufferBarrier.buffer(currentFrame.indirectCommandBuffer.buffer);
                bufferBarrier.offset(0);
                bufferBarrier.size(VK_WHOLE_SIZE);

                vkCmdPipelineBarrier(commandBuffer,
                        VK_PIPELINE_STAGE_DRAW_INDIRECT_BIT, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
                        0, null, bufferBarrier, null);
            }


            if (vkEndCommandBuffer(commandBuffer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to record command buffer");
            }
        }

    }

    public void recordSceneToCommandBufferIndirect(List<Renderable> renderables, VkCommandBuffer commandBuffer, MultiViewRenderPassFrame currentFrame, int frameIndex, MemoryStack stack) {

        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, multiViewGraphicsPipeline);

        var uniformOffset = (int) (padUniformBufferSize(GPUSceneData.SIZEOF, minUniformBufferOffsetAlignment) * frameIndex);
        var pUniformOffset = stack.mallocInt(1);
        pUniformOffset.put(0, uniformOffset);

        vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                multiViewPipelineLayout, 0, stack.longs(currentFrame.cameraAndSceneDescriptorSet), pUniformOffset);

        vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                multiViewPipelineLayout, 1, stack.longs(currentFrame.objectDescriptorSet), null);

        // One vertex buffer and index buffer is used to store all the mesh information
        vkCmdBindVertexBuffers(commandBuffer, 0, stack.longs(mergedVertexBuffer.buffer), stack.longs(0));
        vkCmdBindIndexBuffer(commandBuffer, mergedIndexBuffer.buffer, 0, VK_INDEX_TYPE_UINT32);

        // Probably needs to be static, or better optimized. Will be done when Compute shaders are enabled
        var indirectBatches = compactDraws(renderables);

        logPerSec("Num of indirect Batches: " + indirectBatches.size());

        int stride = VkDrawIndexedIndirectCommand.SIZEOF;
        for(var batch: indirectBatches) {

            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                    multiViewPipelineLayout, 2, stack.longs(renderables.get(batch.first).textureDescriptorSet), null);

            long offset = batch.first * VkDrawIndexedIndirectCommand.SIZEOF;

            vkCmdDrawIndexedIndirect(commandBuffer, currentFrame.indirectCommandBuffer.buffer,
                    offset, batch.count, stride);
        }

    }

    public void performBufferDataUpdates(List<Renderable> renderables, int currentFrameIndex) {
        //TODO: Need to be modified so that buffers are updated only when data is updated,
        // and only parts of buffers are updated as needed

        var frame = multiViewRenderPass.frames.get(currentFrameIndex);

        if(frame.renderablesUpdated) {
        }

        if (frame.shouldUpdateCameraBuffer) {
            updateCameraGPUDataInMemory(currentFrameIndex);
        }
        if (shouldUpdateGPUSceneBuffer) {
            updateSceneGPUDataInMemory(currentFrameIndex);
        }
        if(frame.shouldUpdateObjectBuffer) {
            updateObjectBufferDataInMemory(renderables, frame);
        }
        if(frame.shouldUpdateIndirectCommandsBuffer) {
            updateIndirectCommandBuffer(renderables, frame);
        }
        // This would be toggled else where in the system when a renderable is added or removed
        if(frame.shouldUpdateComputeUboBuffer) {
            updateComputeUBO(frame);
        }

        shouldUpdateGPUSceneBuffer = false;
        frame.shouldUpdateCameraBuffer = false;
        frame.shouldUpdateObjectBuffer = false;
        frame.shouldUpdateIndirectCommandsBuffer = false;
        frame.shouldUpdateComputeUboBuffer = false;
        frame.renderablesUpdated = false;
    }

    public void renderMultiViewFrame(MultiViewRenderPassFrame curMultiViewFrame, MemoryStack stack) {

        var timeLineInfo = VkTimelineSemaphoreSubmitInfo.calloc(stack);
        timeLineInfo.sType(VK_STRUCTURE_TYPE_TIMELINE_SEMAPHORE_SUBMIT_INFO);
        timeLineInfo.pWaitSemaphoreValues(stack.longs(1));
        timeLineInfo.pSignalSemaphoreValues(stack.longs(2)); //Signals timeline semaphore of val=2

        // Submit rendering commands to GPU
        VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
        submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
        submitInfo.pNext(timeLineInfo);
        submitInfo.pSignalSemaphores(stack.longs(curMultiViewFrame.timeLineSemaphore));
        submitInfo.pWaitSemaphores(stack.longs(curMultiViewFrame.timeLineSemaphore));
        submitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT));
        submitInfo.pCommandBuffers(stack.pointers(curMultiViewFrame.commandBuffer));

        int vkResult;
        if(( vkResult = vkQueueSubmit(graphicsQueue, submitInfo, VK_NULL_HANDLE)) != VK_SUCCESS) {
            throw new RuntimeException("Failed to submit draw command buffer: " + vkResult);
        }

    }

    public void drawViewFrame(long timeLineSemaphore, int viewImageToRender) {
        try(MemoryStack stack = stackPush()) {

            vkWaitForFences(device, drawFences.get(currentDisplayBufferIndex), true, UINT64_MAX);
            vkResetFences(device, drawFences.get(currentDisplayBufferIndex));

            var timeLineInfo = VkTimelineSemaphoreSubmitInfo.calloc(stack);
            timeLineInfo.sType(VK_STRUCTURE_TYPE_TIMELINE_SEMAPHORE_SUBMIT_INFO);
            timeLineInfo.pWaitSemaphoreValues(stack.longs(2));

            if(viewImageToRender == 0) {
                timeLineInfo.pSignalSemaphoreValues(stack.longs(3, 0)); //Signals timeline semaphore of val=3
            }
            else {
                timeLineInfo.pSignalSemaphoreValues(stack.longs(4, 0)); //Signals timeline semaphore of val=4
            }

            recordViewCommandBuffer(viewImageToRender);

            // Submit rendering commands to GPU
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.pNext(timeLineInfo);
            submitInfo.waitSemaphoreCount(1);

            submitInfo.pWaitSemaphores(stack.longs(timeLineSemaphore));
            submitInfo.pSignalSemaphores(stack.longs(timeLineSemaphore, renderCompleteSemaphore));
            submitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));
            submitInfo.pCommandBuffers(stack.pointers(drawCmds.get(currentDisplayBufferIndex)));

            int vkResult;
            if((vkResult = vkQueueSubmit(graphicsQueue, submitInfo, drawFences.get(currentDisplayBufferIndex))) != VK_SUCCESS) {
                vkResetFences(device, drawFences.get(currentDisplayBufferIndex));
                throw new RuntimeException("Failed to submit draw command buffer: " + vkResult);
            }

        }
    }

    public void callCompute(MultiViewRenderPassFrame frame, MemoryStack stack) {

        var timeLineInfo = VkTimelineSemaphoreSubmitInfo.calloc(stack);
        timeLineInfo.sType(VK_STRUCTURE_TYPE_TIMELINE_SEMAPHORE_SUBMIT_INFO);
        timeLineInfo.pSignalSemaphoreValues(stack.longs(1));
        timeLineInfo.signalSemaphoreValueCount(1);

        // Submit rendering commands to GPU
        VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
        submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
        submitInfo.pNext(timeLineInfo);
        submitInfo.pSignalSemaphores(stack.longs(frame.timeLineSemaphore));
        submitInfo.pCommandBuffers(stack.pointers(frame.computeCommandBuffer));

        vkCheck(vkQueueSubmit(computeQueue, submitInfo, VK_NULL_HANDLE), "Could not submit to compute queue");
    }

    public void initPipelines() {
        createComputePipeline();
        createMultiViewGraphicsPipeline();
        createViewGraphicsPipeline();
    }

    public void initRenderPasses() {
        createMultiViewRenderPass();
    }

    @Override
    public void initDescriptorSets() {

        for(int i = 0;i < multiViewRenderPass.frames.size(); i++) {
            var multiViewFrame = multiViewRenderPass.frames.get(i);

            // CREATE COMPUTE SHADER DESCRIPTOR SETS
            var result = new DescriptorBuilder(descriptorSetLayoutCache, descriptorAllocator)
                    .bindBuffer(0, new DescriptorBufferInfo(0, VK_WHOLE_SIZE, multiViewFrame.objectBuffer.buffer),
                            VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT)
                    .bindBuffer(1, new DescriptorBufferInfo(0, VK_WHOLE_SIZE, multiViewFrame.indirectCommandBuffer.buffer),
                            VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT)
                    .bindBuffer(2, new DescriptorBufferInfo(0, VK_WHOLE_SIZE, multiViewFrame.computeUBOBuffer.buffer),
                            VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT)
                    .bindBuffer(3, new DescriptorBufferInfo(0, VK_WHOLE_SIZE, multiViewFrame.indirectDrawCountBuffer.buffer),
                            VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT)
                    .build();

            multiViewRenderPass.computeDescriptorSetLayout = result.layout();
            multiViewFrame.computeDescriptorSet = result.descriptorSet();

            // GPU Scene data descriptor set
            result = new DescriptorBuilder(descriptorSetLayoutCache, descriptorAllocator)
                    .bindBuffer(0,
                            new DescriptorBufferInfo(0, VK_WHOLE_SIZE, multiViewFrame.cameraBuffer.buffer),
                            VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, VK_SHADER_STAGE_VERTEX_BIT)
                    .bindBuffer(1,
                            new DescriptorBufferInfo(0, GPUSceneData.SIZEOF, gpuSceneBuffer.buffer),
                            VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC, VK_SHADER_STAGE_VERTEX_BIT)
                    .build();

            multiViewRenderPass.frames.get(i).cameraAndSceneDescriptorSet = result.descriptorSet();
            multiViewRenderPass.cameraAndSceneDescriptorSetLayout = result.layout();

            // Object buffer Descriptor set
            result = new DescriptorBuilder(descriptorSetLayoutCache, descriptorAllocator)
                    .bindBuffer(0,
                            new DescriptorBufferInfo(0, VK_WHOLE_SIZE, multiViewFrame.objectBuffer.buffer),
                            VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_VERTEX_BIT)
                    .build();

            multiViewFrame.objectDescriptorSet = result.descriptorSet();
            multiViewRenderPass.objectDescriptorSetLayout = result.layout();
        }

        imageInputDescriptorSets = new ArrayList<>();
        for(int i = 0; i < swapChainAttachments.size(); i++) {
            // Descriptor set for image input for the view Render pass
            // attaches to the output imageview from the multiview pass
            var result = new DescriptorBuilder(descriptorSetLayoutCache, descriptorAllocator)
                    .bindImage(0,
                            new DescriptorImageInfo(getTextureSampler(1), multiViewRenderPass.colorAttachment.imageView, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL),
                            VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, VK_SHADER_STAGE_FRAGMENT_BIT)
                    .build();

            imageInputDescriptorSets.add(result.descriptorSet());
            textureSetLayout = result.layout();
        }

    }

    private void initComputeCmdPoolsCmdBuffersSyncObjects() {

        try(var stack = stackPush()) {

            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
            poolInfo.queueFamilyIndex(queueFamilyIndices.computeFamily);
            poolInfo.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);

            // temp pointers to allocate commandpools and cmdBuffers
            LongBuffer pCommandPool = stack.mallocLong(1);
            PointerBuffer pCommandBuffers = stack.mallocPointer(1);

            VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack);
            semaphoreInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            for (var frame : multiViewRenderPass.frames) {

                // CREATE COMMAND POOL AND COMMAND BUFFER
                vkCheck(vkCreateCommandPool(device, poolInfo, null, pCommandPool));
                frame.computeCommandPool = pCommandPool.get(0);

                var allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
                allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
                allocInfo.commandPool(frame.computeCommandPool);
                allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
                allocInfo.commandBufferCount(1);

                vkCheck(vkAllocateCommandBuffers(device, allocInfo, pCommandBuffers));
                frame.computeCommandBuffer = new VkCommandBuffer(pCommandBuffers.get(0), device);
            }
        }

    }

    public void updateViewRenderPassImageInputDescriptorSet() {
        try (var stack = stackPush()) {

            for (int i = 0; i < swapChainAttachments.size(); i++) {

                //information about the buffer we want to point at in the descriptor
                var imageBufferInfo = VkDescriptorImageInfo.calloc(1, stack);
                imageBufferInfo.sampler(getTextureSampler(1));
                imageBufferInfo.imageView(multiViewRenderPass.colorAttachment.imageView);
                imageBufferInfo.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

                VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(1, stack);
                var textureWrite =
                        createWriteDescriptorSet(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                                imageInputDescriptorSets.get(i),
                                imageBufferInfo,
                                0, stack);
                descriptorWrites.put(0, textureWrite);

                vkUpdateDescriptorSets(device, descriptorWrites, null);
            }
        }
    }

    public void swapChainRecreatedEvent() {
        vkDestroyFramebuffer(device, multiViewRenderPass.frameBuffer, null);

        vkDestroyImageView(device, multiViewRenderPass.depthAttachment.imageView, null);
        vmaDestroyImage(vmaAllocator, multiViewRenderPass.depthAttachment.allocatedImage.image, multiViewRenderPass.depthAttachment.allocatedImage.allocation);

        vkDestroyImageView(device, multiViewRenderPass.colorAttachment.imageView, null);
        vmaDestroyImage(vmaAllocator, multiViewRenderPass.colorAttachment.allocatedImage.image, multiViewRenderPass.colorAttachment.allocatedImage.allocation);

        createMultiViewColorAttachment();
        createMultiViewDepthAttachment();

        initMultiViewFrameBuffers();

        updateViewRenderPassImageInputDescriptorSet();

        for(int i = 0; i < multiViewRenderPass.frames.size(); i++) {
            recordMultiViewCommandBuffer(renderables, multiViewRenderPass.frames.get(i), i);
        }
    }

    public void createMultiViewDepthAttachment() {
        try(MemoryStack stack = stackPush()) {
            var depthAttachment = new FrameBufferAttachment();

            int depthFormat = findDepthFormat(physicalDevice);
            var extent = VkExtent3D.calloc(stack).width(swapChainExtent.width()).height(swapChainExtent.height()).depth(1);

            var imageInfo = createImageCreateInfo(
                    depthFormat,
                    VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT,
                    extent,
                    1,
                    VK_IMAGE_TILING_OPTIMAL,
                    multiViewNumLayers,
                    1,
                    stack);

            var memoryAllocInfo = VmaAllocationCreateInfo.calloc(stack)
                    .usage(VMA_MEMORY_USAGE_GPU_ONLY)
                    .requiredFlags(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

            depthAttachment.allocatedImage = createImage(imageInfo, memoryAllocInfo, vmaAllocator);

            var viewInfo =
                    createImageViewCreateInfo(
                            depthFormat,
                            depthAttachment.allocatedImage.image,
                            VK_IMAGE_ASPECT_DEPTH_BIT,
                            1,
                            multiViewNumLayers,
                            VK_IMAGE_VIEW_TYPE_2D_ARRAY,
                            stack
                    );

            depthAttachment.imageView = createImageView(viewInfo, device);

            // Explicitly transitioning the depth image
            submitImmediateCommand((cmd) -> {
                        transitionImageLayout(
                                depthAttachment.allocatedImage.image, depthFormat,
                                VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
                                1,multiViewNumLayers, cmd);
                    },
                    singleTimeGraphicsCommandContext);

            multiViewRenderPass.depthAttachment = depthAttachment;
        }
    }

    public void createMultiViewRenderPass() {
        try(MemoryStack stack = stackPush()) {

            VkAttachmentDescription.Buffer attachments;
            VkAttachmentReference.Buffer attachmentRefs;

            attachments = VkAttachmentDescription.calloc(2, stack);
            attachmentRefs = VkAttachmentReference.calloc(2, stack);

            // Color attachment
            var colorAttachment = attachments.get(0);
            colorAttachment.format(swapChainImageFormat);
            colorAttachment.samples(numOfSamples);
            colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
            colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
            colorAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
            colorAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            colorAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            colorAttachment.finalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

            var colorAttachmentRef = attachmentRefs.get(0);
            colorAttachmentRef.attachment(0);
            colorAttachmentRef.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            // Depth-Stencil attachments
            VkAttachmentDescription depthAttachment = attachments.get(1);
            depthAttachment.format(findDepthFormat(physicalDevice));
            depthAttachment.samples(numOfSamples);
            depthAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
            depthAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
            depthAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
            depthAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            depthAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            depthAttachment.finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

            VkAttachmentReference depthAttachmentRef = attachmentRefs.get(1);
            depthAttachmentRef.attachment(1);
            depthAttachmentRef.layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

            VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1, stack);
            subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);
            subpass.colorAttachmentCount(1);
            subpass.pColorAttachments(VkAttachmentReference.calloc(1, stack).put(0, colorAttachmentRef));
            subpass.pDepthStencilAttachment(depthAttachmentRef);

            VkSubpassDependency.Buffer dependencies = VkSubpassDependency.calloc(2, stack);

            var dependency1 = dependencies.get(0);
            dependency1.srcSubpass(VK_SUBPASS_EXTERNAL);
            dependency1.dstSubpass(0);

            dependency1.srcStageMask(VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT);
            dependency1.dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);

            dependency1.srcAccessMask(VK_ACCESS_MEMORY_READ_BIT);
            dependency1.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
            dependency1.dependencyFlags(VK_DEPENDENCY_BY_REGION_BIT);

            var dependency2 = dependencies.get(1);
            dependency2.srcSubpass(0);
            dependency2.dstSubpass(VK_SUBPASS_EXTERNAL);

            dependency2.srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
            dependency2.dstStageMask(VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT);

            dependency2.srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
            dependency2.dstAccessMask(VK_ACCESS_MEMORY_READ_BIT);
            dependency2.dependencyFlags(VK_DEPENDENCY_BY_REGION_BIT);

            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack);
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO);
            renderPassInfo.pAttachments(attachments);
            renderPassInfo.pSubpasses(subpass);
            renderPassInfo.pDependencies(dependencies);

            var viewMask = stack.ints(Integer.parseInt("00000011", 2));
            var correlationMask = stack.ints(Integer.parseInt("00000011", 2));

            var multiviewCreateInfo = VkRenderPassMultiviewCreateInfo.calloc();
            multiviewCreateInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_MULTIVIEW_CREATE_INFO);
            multiviewCreateInfo.pViewMasks(viewMask);
            multiviewCreateInfo.pCorrelationMasks(correlationMask);

            renderPassInfo.pNext(multiviewCreateInfo);

            LongBuffer pRenderPass = stack.mallocLong(1);

            if(vkCreateRenderPass(device, renderPassInfo, null, pRenderPass) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create render pass");
            }

            multiViewRenderPass.renderPass = pRenderPass.get(0);
            deletionQueue.add(() -> vkDestroyRenderPass(device, multiViewRenderPass.renderPass, null));
        }
    }

    public void createComputePipeline() {

        var builder = new PipelineBuilder(PipelineBuilder.PipelineType.COMPUTE);

        builder.shaderStages.add(new PipelineBuilder.ShaderStageCreateInfo("shaders/cull.comp", VK_SHADER_STAGE_COMPUTE_BIT));
        builder.descriptorSetLayouts = new long[]{multiViewRenderPass.computeDescriptorSetLayout};
        var result = builder.build(device, null);

        computePipelineLayout = result.pipelineLayout();
        computePipeline = result.pipeline();

        deletionQueue.add(() -> vkDestroyPipeline(device, computePipeline, null));
        deletionQueue.add(() -> vkDestroyPipelineLayout(device, computePipelineLayout, null));

    }

    public void createMultiViewGraphicsPipeline() {

        var builder = new PipelineBuilder();

        // Vertex attribute description
        var attribs = new ArrayList<PipelineBuilder.VertexAttributeDescription>();
        attribs.add(new PipelineBuilder.VertexAttributeDescription(0, 0, VK_FORMAT_R32G32B32_SFLOAT, 0)); //pos
        attribs.add(new PipelineBuilder.VertexAttributeDescription(0, 1, VK_FORMAT_R32G32_SFLOAT, 3 * Float.BYTES)); //tex
        attribs.add(new PipelineBuilder.VertexAttributeDescription(0, 2, VK_FORMAT_R32G32B32_SFLOAT, 5 * Float.BYTES)); //normal

        builder.vertexAttributeDescriptions = attribs;
        builder.vertexBindingDescription = new PipelineBuilder.VertexBindingDescription(0, (3 + 2 + 3) * Float.BYTES, VK_VERTEX_INPUT_RATE_VERTEX);
        builder.inputAssemblyCreateInfo = new PipelineBuilder.InputAssemblyCreateInfo(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST, false);
        builder.dynamicStates.add(VK_DYNAMIC_STATE_VIEWPORT);
        builder.dynamicStates.add(VK_DYNAMIC_STATE_SCISSOR);

        builder.shaderStages.add(new PipelineBuilder.ShaderStageCreateInfo("shaders/multiview.vert", VK_SHADER_STAGE_VERTEX_BIT));
        builder.shaderStages.add(new PipelineBuilder.ShaderStageCreateInfo("shaders/multiview.frag", VK_SHADER_STAGE_FRAGMENT_BIT));
        builder.descriptorSetLayouts = new long[]{multiViewRenderPass.cameraAndSceneDescriptorSetLayout, multiViewRenderPass.objectDescriptorSetLayout, textureSetLayout};

        var pipeLineCreateResults = builder.build(device, multiViewRenderPass.renderPass);
        multiViewPipelineLayout = pipeLineCreateResults.pipelineLayout();
        multiViewPipelineLayout = pipeLineCreateResults.pipelineLayout();
        multiViewGraphicsPipeline = pipeLineCreateResults.pipeline();

        deletionQueue.add(() -> vkDestroyPipeline(device, multiViewGraphicsPipeline, null));
        deletionQueue.add(() -> vkDestroyPipelineLayout(device, multiViewPipelineLayout, null));
    }

    public void createViewGraphicsPipeline() {
        var builder = new PipelineBuilder();
        builder.dynamicStates.add(VK_DYNAMIC_STATE_VIEWPORT);
        builder.dynamicStates.add(VK_DYNAMIC_STATE_SCISSOR);

        builder.shaderStages.add(new PipelineBuilder.ShaderStageCreateInfo("shaders/ActiveShutterView.vert", VK_SHADER_STAGE_VERTEX_BIT));
        builder.shaderStages.add(new PipelineBuilder.ShaderStageCreateInfo("shaders/ActiveShutterView.frag", VK_SHADER_STAGE_FRAGMENT_BIT));
        builder.descriptorSetLayouts = new long[]{textureSetLayout};
        builder.pushConstant = new PipelineBuilder.PushConstant(0, Float.BYTES, VK_SHADER_STAGE_FRAGMENT_BIT);
        builder.depthStencil = new PipelineBuilder.PipelineDepthStencilStateCreateInfo(false, false, VK_COMPARE_OP_LESS_OR_EQUAL, false, false);
        builder.rasterizer = new PipelineBuilder.PipelineRasterizationStateCreateInfo(VK_CULL_MODE_FRONT_BIT, VK_FRONT_FACE_COUNTER_CLOCKWISE);
        builder.colorAttachmentImageFormat = swapChainImageFormat;
        
        var pipeLineCreateResults = builder.build(device, null);
        pipelineLayout = pipeLineCreateResults.pipelineLayout();
        graphicsPipeline = pipeLineCreateResults.pipeline();

        deletionQueue.add(() -> vkDestroyPipeline(device, graphicsPipeline, null));
        deletionQueue.add(() -> vkDestroyPipelineLayout(device, pipelineLayout, null));
    }

    public void initMultiViewFrameBuffers() {

        try(MemoryStack stack = stackPush()) {

            var attachments = stack.longs(VK_NULL_HANDLE);
            LongBuffer pFramebuffer = stack.mallocLong(1);

            VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.calloc(stack);
            framebufferInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
            framebufferInfo.renderPass(renderPass);
            framebufferInfo.width(swapChainExtent.width());
            framebufferInfo.height(swapChainExtent.height());
            framebufferInfo.layers(1);

            // Create single frameBuffer for multiview renderpass
            framebufferInfo.renderPass(multiViewRenderPass.renderPass);
            attachments = stack.longs(multiViewRenderPass.colorAttachment.imageView, multiViewRenderPass.depthAttachment.imageView);
            framebufferInfo.pAttachments(attachments);

            if(vkCreateFramebuffer(device, framebufferInfo, null, pFramebuffer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create framebuffer");
            }

            multiViewRenderPass.frameBuffer = pFramebuffer.get(0);
        }
    }

    public void createMultiViewColorAttachment() {
        try(var stack = stackPush()) {

            var colorAttachment = new FrameBufferAttachment();

            var extent = VkExtent3D.calloc(stack).width(swapChainExtent.width()).height(swapChainExtent.height()).depth(1);

            var imageInfo = createImageCreateInfo(
                    swapChainImageFormat,
                    VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
                    extent,
                    1,
                    VK_IMAGE_TILING_OPTIMAL,
                    multiViewNumLayers,
                    1,
                    stack);

            var memoryAllocInfo = VmaAllocationCreateInfo.calloc(stack)
                    .usage(VMA_MEMORY_USAGE_GPU_ONLY)
                    .requiredFlags(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

            colorAttachment.allocatedImage = createImage(imageInfo, memoryAllocInfo, vmaAllocator);

            var viewInfo =
                    createImageViewCreateInfo(
                            swapChainImageFormat,
                            colorAttachment.allocatedImage.image,
                            VK_IMAGE_ASPECT_COLOR_BIT,
                            1,
                            multiViewNumLayers,
                            VK_IMAGE_VIEW_TYPE_2D_ARRAY,
                            stack
                    );

            colorAttachment.imageView = createImageView(viewInfo, device);

            submitImmediateCommand((cmd) -> {
                transitionImageLayout(
                        colorAttachment.allocatedImage.image, swapChainImageFormat,
                        VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                        1, multiViewNumLayers, cmd);
            }, singleTimeGraphicsCommandContext);

            multiViewRenderPass.colorAttachment = colorAttachment;
        }
    }

    private void updateComputeUBO(MultiViewRenderPassFrame currentFrame) {
        var bw = new BufferWriter(vmaAllocator, currentFrame.computeUBOBuffer, ComputeUBOIn.SIZEOF, ComputeUBOIn.SIZEOF);
        bw.mapBuffer();
        ComputeUBOIn.memcpy(bw.buffer, computeUBOIn);
        bw.unmapBuffer();
    }

    public void updateCameraGPUDataInMemory(int currentFrame) {
        var alignment = GPUCameraData.SIZEOF;
        var bufferSize = alignment * multiViewNumLayers;

        var bw = new BufferWriter(vmaAllocator, multiViewRenderPass.frames.get(currentFrame).cameraBuffer, alignment, bufferSize);
        bw.mapBuffer();
        GPUCameraData.memcpy(bw.buffer, gpuCameraDataLeft);
        bw.setPosition(1);
        GPUCameraData.memcpy(bw.buffer, gpuCameraDataRight);
        bw.unmapBuffer();
    }

    public void updateSceneGPUDataInMemory(int currentFrame) {
        var alignment = (int)padUniformBufferSize(GPUSceneData.SIZEOF, minUniformBufferOffsetAlignment);
        int bufferSize = alignment * MAX_FRAMES_IN_FLIGHT;

        var bw = new BufferWriter(vmaAllocator, gpuSceneBuffer, alignment, bufferSize);
        bw.mapBuffer();
        bw.setPosition(currentFrame);
        GPUSceneData.memcpy(bw.buffer, gpuSceneData);
        bw.unmapBuffer();
    }

    public void updateObjectBufferDataInMemory(List<Renderable> renderables, MultiViewRenderPassFrame frame) {

        //        TODO: Check whether buffer must be resized

        var alignmentSize = (int)(padUniformBufferSize(GPUObjectData.SIZEOF, minUniformBufferOffsetAlignment));
        alignmentSize = GPUObjectData.SIZEOF;
        int bufferSize = alignmentSize * renderables.size();

        var bw = new BufferWriter(vmaAllocator, frame.objectBuffer, alignmentSize, bufferSize);
        bw.mapBuffer();
        for(int i = 0; i < renderables.size(); i++) {
            var renderable = renderables.get(i);
            if(renderable.isDirty) {
                bw.setPosition(i);
                bw.put(renderable.model.objectToWorldMatrix);
                bw.put(renderable.model.getScale().getNorm() * renderable.mesh.boundingRadius);
                renderable.isDirty = false;
            }
        }
        bw.unmapBuffer();
    }

    public void updateIndirectCommandBuffer(List<Renderable> renderables, MultiViewRenderPassFrame frame) {

//        TODO: Check whether buffer must be resized

        var alignmentSize = VkDrawIndexedIndirectCommand.SIZEOF;
        var bufferSize = alignmentSize * renderables.size();

        var stagingBuffer = createBufferVMA(vmaAllocator,
                bufferSize,
                VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                VMA_MEMORY_USAGE_AUTO,
                VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT);

        var bw = new BufferWriter(vmaAllocator, stagingBuffer, alignmentSize, bufferSize);
        bw.mapBuffer();
        for(int i = 0; i < renderables.size(); i++) {
            bw.setPosition(i);

//            uint32_t    indexCount;
//            uint32_t    instanceCount;
//            uint32_t    firstIndex;
//            int32_t     vertexOffset;
//            uint32_t    firstInstance;

            bw.put(renderables.get(i).mesh.indices.size());
            bw.put(1);
            bw.put(renderables.get(i).firstIndex);
            bw.put(renderables.get(i).firstVertex);
            bw.put(i);
        }
        bw.unmapBuffer();

        try (var stack = stackPush()) {
            var copy = VkBufferCopy.calloc(1, stack);

            Consumer<VkCommandBuffer> copyCmd = cmd -> {
                copy.dstOffset(0);
                copy.size(bufferSize);
                copy.srcOffset(0);
                vkCmdCopyBuffer(cmd, stagingBuffer.buffer, frame.indirectCommandBuffer.buffer, copy);
            };

            submitImmediateCommand(copyCmd, singleTimeTransferCommandContext);
        }

        vmaDestroyBuffer(vmaAllocator, stagingBuffer.buffer, stagingBuffer.allocation);
    }

    public void initMultiViewCmdPoolsAndBuffers() {

        try (var stack = stackPush()) {

            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
            poolInfo.queueFamilyIndex(queueFamilyIndices.graphicsFamily);
            poolInfo.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);

            for (int i = 0; i < multiViewRenderPass.frames.size(); i++) {

                LongBuffer pCommandPool = stack.mallocLong(1);
                PointerBuffer pCommandBuffers = stack.mallocPointer(1);

                // Create command pool and command buffer for MultiView RenderPass
                if (vkCreateCommandPool(device, poolInfo, null, pCommandPool) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create command pool");
                }
                var commandPool = pCommandPool.get(0);

                // Create command buffer
                var allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
                allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
                allocInfo.commandPool(commandPool);
                allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
                allocInfo.commandBufferCount(1);

                pCommandBuffers = stack.mallocPointer(1);
                if (vkAllocateCommandBuffers(device, allocInfo, pCommandBuffers) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to allocate command buffers");
                }

                var commandBuffer = new VkCommandBuffer(pCommandBuffers.get(0), device);

                multiViewRenderPass.frames.get(i).commandPool = commandPool;
                multiViewRenderPass.frames.get(i).commandBuffer = commandBuffer;

            }
        }
    }

    public long resetTimelineSemaphore(long oldTimelineSemaphore) {

        try (var stack = stackPush()) {
            vkDestroySemaphore(device, oldTimelineSemaphore, null);

            LongBuffer timeLineSemaphore = stack.mallocLong(1);
            var timelineCreateInfo = VkSemaphoreTypeCreateInfo.calloc(stack);
            timelineCreateInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_TYPE_CREATE_INFO);
            timelineCreateInfo.semaphoreType(VK_SEMAPHORE_TYPE_TIMELINE);
            timelineCreateInfo.initialValue(0);

            VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack);
            semaphoreInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
            semaphoreInfo.pNext(timelineCreateInfo);
            vkCheck(vkCreateSemaphore(device, semaphoreInfo, null, timeLineSemaphore));

            return timeLineSemaphore.get(0);
        }

    }

    public void initMultiViewSyncObjects() {

        try (MemoryStack stack = stackPush()) {

            VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack);
            semaphoreInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            LongBuffer timeLineSemaphore = stack.mallocLong(1);

            var timelineCreateInfo = VkSemaphoreTypeCreateInfo.calloc(stack);
            timelineCreateInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_TYPE_CREATE_INFO);
            timelineCreateInfo.semaphoreType(VK_SEMAPHORE_TYPE_TIMELINE);
            timelineCreateInfo.initialValue(4);
            semaphoreInfo.pNext(timelineCreateInfo);

            for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
                vkCheck(vkCreateSemaphore(device, semaphoreInfo, null, timeLineSemaphore));
                multiViewRenderPass.frames.get(i).timeLineSemaphore = timeLineSemaphore.get(0);
            }
        }
    }

    @Override
    public void cleanUp() {
        // Wait for the device to complete all operations before release resources
        vkDeviceWaitIdle(device);

        vkDestroyFramebuffer(device, multiViewRenderPass.frameBuffer, null);

        vkDestroyImageView(device, multiViewRenderPass.depthAttachment.imageView, null);
        vmaDestroyImage(vmaAllocator, multiViewRenderPass.depthAttachment.allocatedImage.image, multiViewRenderPass.depthAttachment.allocatedImage.allocation);

        vkDestroyImageView(device, multiViewRenderPass.colorAttachment.imageView, null);
        vmaDestroyImage(vmaAllocator, multiViewRenderPass.colorAttachment.allocatedImage.image, multiViewRenderPass.colorAttachment.allocatedImage.allocation);

        cleanUpSwapChainAndSwapImages();
        for(int i = deletionQueue.size()-1; i >= 0; i--) {
            deletionQueue.get(i).run();
        }
    }

    public static class GPUCameraData {

        public static final int SIZEOF = (3 * 16) * Float.BYTES;
        public Matrix projWorldToCam;
        public Matrix worldToCam;
        public Matrix proj;

        public GPUCameraData() {
            projWorldToCam = Matrix.getIdentityMatrix(4);
            worldToCam = Matrix.getIdentityMatrix(4);
            proj = Matrix.getIdentityMatrix(4);
        }

        public static void memcpy(ByteBuffer buffer, GPUCameraData data) {
            data.projWorldToCam.setValuesToBuffer(buffer);
            data.worldToCam.setValuesToBuffer(buffer);
            data.proj.setValuesToBuffer(buffer);
        }

    }

    public static class ComputeUBOIn {
        public static final int SIZEOF = Float.BYTES * 4 * 6 + Integer.BYTES;
        public List<Vector> frustumPlanes;
        public int objectCount = 0;

        public ComputeUBOIn() {
            frustumPlanes = new ArrayList<>();
            for(int i = 0; i < 6; i++) {
                frustumPlanes.add(new Vector(4, 1.0f));
            }
        }

        public static void memcpy(ByteBuffer buffer, ComputeUBOIn data) {
            for(var f: data.frustumPlanes) {
                f.setValuesToBuffer(buffer);
            }

            buffer.putInt(data.objectCount);
        }
    }

    public static class GPUSceneData {
        public static final int SIZEOF = Float.BYTES * 4 * 5;
        public Vector fogColor; // w is for exponent
        public Vector fogDistance; //x for min, y for max, zw unused.
        public Vector ambientColor;
        public Vector sunlightDirection; // w for sun power
        public Vector sunLightColor;

        public static void memcpy(ByteBuffer buffer, GPUSceneData data) {
            data.fogColor.setValuesToBuffer(buffer);
            data.fogDistance.setValuesToBuffer(buffer);
            data.ambientColor.setValuesToBuffer(buffer);
            data.sunlightDirection.setValuesToBuffer(buffer);
            data.sunLightColor.setValuesToBuffer(buffer);
        }

    }

    public static class GPUObjectData {
        public static final int SIZEOF = Float.BYTES * (16 + 4);
    }

    public class MultiViewRenderPassFrame {
        public long timeLineSemaphore;
        public long commandPool;
        public long computeCommandPool;
        public VkCommandBuffer computeCommandBuffer;
        public long computeDescriptorSet;
        public VkCommandBuffer commandBuffer;
        public AllocatedBuffer cameraBuffer;
        public AllocatedBuffer objectBuffer;
        public AllocatedBuffer indirectCommandBuffer;
        public AllocatedBuffer indirectDrawCountBuffer;
        public AllocatedBuffer computeUBOBuffer;

        // Global Descriptor set contains the camera data and other scene parameters
        public long cameraAndSceneDescriptorSet;

        // This contains the object transformation matrices
        public long objectDescriptorSet;

        public long vmaAllocator;

        public boolean shouldUpdateIndirectCommandsBuffer = true;
        public boolean shouldUpdateObjectBuffer = true;
        public boolean shouldUpdateCameraBuffer = true;
        public boolean shouldUpdateComputeUboBuffer = true;
        public boolean renderablesUpdated = true;

        public void cleanUp() {
            vkDestroySemaphore(device, timeLineSemaphore, null);
            vkDestroyCommandPool(device, commandPool, null);
            vkDestroyCommandPool(device, computeCommandPool, null);
            vmaDestroyBuffer(vmaAllocator, cameraBuffer.buffer, cameraBuffer.allocation);
            vmaDestroyBuffer(vmaAllocator, objectBuffer.buffer, objectBuffer.allocation);
            vmaDestroyBuffer(vmaAllocator, indirectCommandBuffer.buffer, indirectCommandBuffer.allocation);
            vmaDestroyBuffer(vmaAllocator, indirectDrawCountBuffer.buffer, indirectDrawCountBuffer.allocation);
            vmaDestroyBuffer(vmaAllocator, computeUBOBuffer.buffer, computeUBOBuffer.allocation);
        }
    }

}
