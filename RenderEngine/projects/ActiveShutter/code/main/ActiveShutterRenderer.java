package main;

import Kurama.Math.Matrix;
import Kurama.Math.Vector;
import Kurama.Mesh.Mesh;
import Kurama.Vulkan.*;
import Kurama.display.DisplayVulkan;
import Kurama.game.Game;
import Kurama.renderingEngine.RenderingEngine;
import Kurama.scene.Scene;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static Kurama.Vulkan.RenderUtils.compactDraws;
import static Kurama.Vulkan.VulkanUtilities.*;
import static Kurama.utils.Logger.log;
import static java.util.stream.Collectors.toSet;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.KHRMultiview.VK_KHR_MULTIVIEW_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.VK_STRUCTURE_TYPE_RENDER_PASS_MULTIVIEW_CREATE_INFO;
import static org.lwjgl.vulkan.VK12.*;

public class ActiveShutterRenderer extends RenderingEngine {

    public Set<String> DEVICE_EXTENSIONS =
            Stream.of(VK_KHR_SWAPCHAIN_EXTENSION_NAME,
                            VK_KHR_MULTIVIEW_EXTENSION_NAME)
                    .collect(toSet());

    public int MAXOBJECTS = 10000;
    public int MAXCOMMANDS = 10000;
    public static final int MAX_FRAMES_IN_FLIGHT = 2;
    public long surface;
    public int numOfSamples = VK_SAMPLE_COUNT_1_BIT;
    public long minUniformBufferOffsetAlignment = 64;
    public VkPhysicalDeviceProperties gpuProperties;
    public AllocatedBuffer gpuSceneBuffer;
    public QueueFamilyIndices queueFamilyIndices;
    public VkQueue graphicsQueue;
    public VkQueue presentQueue;
    public VkQueue computeQueue;
    public VkQueue transferQueue;
    public long swapChain;

    public DescriptorAllocator descriptorAllocator = new DescriptorAllocator();
    public DescriptorSetLayoutCache descriptorSetLayoutCache = new DescriptorSetLayoutCache();

    public AllocatedBuffer mergedVertexBuffer;
    public AllocatedBuffer mergedIndexBuffer;

    public static class RenderPass {

        public List<ViewRenderPassFrame> frames;
        public Map<Integer, ViewRenderPassFrame> imagesToFrameMap;
        public List<Long> frameBuffers;
        public List<SwapChainAttachment> swapChainAttachments;
        public long renderPass;
    }

    public int swapChainImageFormat;
    public VkExtent2D swapChainExtent;

    public static class MultiViewRenderPass {

        public long frameBuffer;
        public List<MultiViewRenderPassFrame> frames;
        public FrameBufferAttachment depthAttachment;
        public FrameBufferAttachment colorAttachment;
        public long renderPass;

        // Global Descriptor set contains the camera data and other scene parameters
        public long cameraAndSceneDescriptorSetLayout;
        public long computeDescriptorSetLayout;

        // This contains the object transformation matrices
        public long objectDescriptorSetLayout;
    }

    public RenderPass viewRenderPass = new RenderPass();
    public MultiViewRenderPass multiViewRenderPass = new MultiViewRenderPass();
    public int currentMultiViewFrameIndex = 0;

    public long viewPipelineLayout;
    public long viewGraphicsPipeline;
    public long multiViewPipelineLayout;
    public long multiViewGraphicsPipeline;
    public long computePipelineLayout;
    public long computePipeline;

    // The global Command pool and buffer are currently used for tasks such as image loading and transformations
    public long globalCommandPool;
    public VkCommandBuffer globalCommandBuffer;
    public long textureSetLayout;
    public boolean framebufferResize;
    public GPUCameraData gpuCameraDataLeft;
    public GPUCameraData gpuCameraDataRight;
    public GPUSceneData gpuSceneData;
    public SingleTimeCommandContext singleTimeGraphicsCommandContext;
    public SingleTimeCommandContext singleTimeTransferCommandContext;
    public HashMap<String, TextureVK> preparedTextures = new HashMap<>();;
    public HashMap<Integer, Long> textureSamplerToMaxLOD = new HashMap<>();
    public HashMap<String, Long> textureFileToDescriptorSet = new HashMap<>();
    public int multiViewNumLayers = 2;
    public boolean msaaEnabled = false;

    public long vmaAllocator;
    public DisplayVulkan display;
    public ActiveShutterGame game;
    public boolean shouldUpdateGPUSceneBuffer = true;
    public boolean shouldUpdateCameraBuffer = true;

    public ActiveShutterRenderer(Game game) {
        super(game);
        this.game = (ActiveShutterGame) game;
    }

    @Override
    public void init(Scene scene) {
        this.display = game.display;
        initVulkan();
    }

    public void render(List<Renderable> renderables) {
        draw(renderables);
    }

    public void setCameraUpdated() {
        for(var frame: multiViewRenderPass.frames) {
            frame.shouldUpdateCameraBuffer = true;
        }
    }

    public void initVulkan() {
        VulkanUtilities.createInstance("Vulkan game", "Kurama Engine");
        VulkanUtilities.setupDebugMessenger();

        surface = createSurface(instance, display.window);
        physicalDevice = pickPhysicalDevice(instance, surface, DEVICE_EXTENSIONS);
        createLogicalDevice();

        descriptorAllocator.init(device);
        descriptorSetLayoutCache.init(device);

        deletionQueue.add(() -> descriptorSetLayoutCache.cleanUp());
        deletionQueue.add(() -> descriptorAllocator.cleanUp());

        gpuProperties = getGPUProperties(physicalDevice);
        numOfSamples = getMaxUsableSampleCount(gpuProperties);
        numOfSamples = 1;

        minUniformBufferOffsetAlignment = getMinBufferOffsetAlignment(gpuProperties);

        vmaAllocator = createAllocator(physicalDevice, device, instance);

        initializeFrames();
        initSyncObjects();
        createFrameCommandPoolsAndCommandBuffers();
        createBuffers();

        createGlobalCommandPool();
        createGlobalCommandBuffer();

        createSwapChain();
        createSwapChainImageViews();

        createMultiViewColorAttachment();
        createMultiViewDepthAttachment();

        createRenderPasses();

        createFramebuffers();

        prepareCompute();

        // Descriptor set layout is needed when both defining the pipelines, and when creating the descriptor sets
        initDescriptorSets();
        deletionQueue.add(() -> vmaDestroyBuffer(vmaAllocator, gpuSceneBuffer.buffer, gpuSceneBuffer.allocation));
        deletionQueue.add(() -> multiViewRenderPass.frames.forEach(MultiViewRenderPassFrame::cleanUp));
        deletionQueue.add(() -> viewRenderPass.frames.forEach(ViewRenderPassFrame::cleanUp));

        createViewGraphicsPipeline();
        createMultiViewGraphicsPipeline();
        createComputePipeline();

        deletionQueue.add(() -> cleanupSwapChain());
    }

    public void createBuffers() {

        var objectBufferSize = (int)(padUniformBufferSize(GPUObjectData.SIZEOF, minUniformBufferOffsetAlignment)) * MAXOBJECTS;
        var cameraBufferSize = (int)(padUniformBufferSize(GPUCameraData.SIZEOF, minUniformBufferOffsetAlignment)) * multiViewNumLayers;

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
                    MAXCOMMANDS * VkDrawIndexedIndirectCommand.SIZEOF,
                    VK_BUFFER_USAGE_STORAGE_BUFFER_BIT |  VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT,
                    VMA_MEMORY_USAGE_AUTO,
                    VMA_ALLOCATION_CREATE_DEDICATED_MEMORY_BIT
            );

            frame.indirectDrawCountBuffer = createBufferVMA(
                    vmaAllocator,
                    Integer.BYTES,
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

    public void recordViewCommandBuffer(ViewRenderPassFrame currentFrame, long frameBuffer, int viewImageToRender) {

        var commandBuffer = currentFrame.commandBuffer;

        try (var stack = stackPush()) {
            if(vkResetCommandBuffer(commandBuffer, 0) != VK_SUCCESS) {
                throw new RuntimeException("Failed to begin recording command buffer");
            }

            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

            VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc(stack);
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO);

            renderPassInfo.renderPass(viewRenderPass.renderPass);

            VkRect2D renderArea = VkRect2D.calloc(stack);
            renderArea.offset(VkOffset2D.calloc(stack).set(0, 0));
            renderArea.extent(swapChainExtent);
            renderPassInfo.renderArea(renderArea);

            VkClearValue.Buffer clearValues = VkClearValue.calloc(1, stack);
            clearValues.get(0).color().float32(stack.floats(0.0f, 0.0f, 0.0f, 1.0f));
            renderPassInfo.pClearValues(clearValues);

            if (vkBeginCommandBuffer(commandBuffer, beginInfo) != VK_SUCCESS) {
                throw new RuntimeException("Failed to begin recording command buffer");
            }

            renderPassInfo.framebuffer(frameBuffer);

            vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);
            {
                // Bind color Attachment from previous multiview Renderpass as input
                vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                        viewPipelineLayout, 0,
                        stack.longs(currentFrame.imageInputDescriptorSet), null);

                vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, viewGraphicsPipeline);

                vkCmdPushConstants(commandBuffer,
                        viewPipelineLayout,
                        VK_SHADER_STAGE_FRAGMENT_BIT,
                        0,
                        stack.floats(viewImageToRender));

                // Render a fullscreen triangle, so that the fragment shader is run for each pixel
                vkCmdDraw(commandBuffer, 3, 1,0, 0);

            }
            vkCmdEndRenderPass(commandBuffer);


            if (vkEndCommandBuffer(commandBuffer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to record command buffer");
            }

        }

    }

    public void recordMultiViewCommandBuffer(List<Renderable> renderables, MultiViewRenderPassFrame currentFrame, long frameBuffer, int frameIndex) {

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

            renderPassInfo.framebuffer(frameBuffer);

            vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);
            {
//                recordSceneToCommandBuffer(renderables, commandBuffer,currentFrame, frameIndex, stack);
                recordSceneToCommandBufferIndirect(renderables, commandBuffer, currentFrame, frameIndex, stack);
            }
            vkCmdEndRenderPass(commandBuffer);


            if (vkEndCommandBuffer(commandBuffer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to record command buffer");
            }
        }

    }

    public void recordSceneToCommandBuffer(List<Renderable> renderables, VkCommandBuffer commandBuffer, MultiViewRenderPassFrame currentFrame, int frameIndex, MemoryStack stack) {

        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, multiViewGraphicsPipeline);

        var uniformOffset = (int) (padUniformBufferSize(GPUSceneData.SIZEOF, minUniformBufferOffsetAlignment) * frameIndex);
        var pUniformOffset = stack.mallocInt(1);
        pUniformOffset.put(0, uniformOffset);

        vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                multiViewPipelineLayout, 0, stack.longs(currentFrame.cameraAndSceneDescriptorSet), pUniformOffset);

        vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                multiViewPipelineLayout, 1, stack.longs(currentFrame.objectDescriptorSet), null);

        vkCmdBindVertexBuffers(commandBuffer, 0, stack.longs(mergedVertexBuffer.buffer), stack.longs(0));
        vkCmdBindIndexBuffer(commandBuffer, mergedIndexBuffer.buffer, 0, VK_INDEX_TYPE_UINT32);

        Mesh previousMesh = null;
        Long previousTextureSet = null;

        for(int i = 0; i < renderables.size(); i++) {
            var renderable = renderables.get(i);

            // Bind texture descriptor set only if it is different from previous texture
            if(previousTextureSet != renderable.textureDescriptorSet) {
                vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                        multiViewPipelineLayout, 2, stack.longs(renderable.textureDescriptorSet), null);

                previousTextureSet = renderable.textureDescriptorSet;
            }

            vkCmdDrawIndexed(commandBuffer, renderable.mesh.indices.size(), 1, renderable.firstIndex, renderable.firstVertex, i);
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

        for(var batch: indirectBatches) {

            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                    multiViewPipelineLayout, 2, stack.longs(renderables.get(batch.first).textureDescriptorSet), null);

            long offset = batch.first * VkDrawIndexedIndirectCommand.SIZEOF;
            int stride = VkDrawIndexedIndirectCommand.SIZEOF;

            vkCmdDrawIndexedIndirect(commandBuffer, currentFrame.indirectCommandBuffer.buffer,
                    offset, batch.count, stride);
        }

    }

    public long getTextureSampler(int maxLod) {
        textureSamplerToMaxLOD.computeIfAbsent(maxLod, TextureVK::createTextureSampler);
        return textureSamplerToMaxLOD.get(maxLod);
    }

    public void prepareTexture(TextureVK texture) {
        if (texture == null || texture.fileName == null || preparedTextures.containsKey(texture.fileName)) return;

        TextureVK.createTextureImage(vmaAllocator, singleTimeGraphicsCommandContext, texture);
        TextureVK.createTextureImageView(texture);
        texture.textureSampler = getTextureSampler(texture.mipLevels);

        preparedTextures.put(texture.fileName, texture);
    }

    public void performBufferDataUpdates(List<Renderable> renderables, int currentFrameIndex) {
        //TODO: Need to be modified so that buffers are updated only when data is updated,
        // and only parts of buffers are updated as needed

        if (multiViewRenderPass.frames.get(currentFrameIndex).shouldUpdateCameraBuffer) {
            updateCameraGPUDataInMemory(currentFrameIndex);
        }
        if (shouldUpdateGPUSceneBuffer) {
            updateSceneGPUDataInMemory(currentFrameIndex);
        }
        if(multiViewRenderPass.frames.get(currentFrameIndex).shouldUpdateObjectBuffer) {
            updateObjectBufferDataInMemory(renderables, multiViewRenderPass.frames.get(currentFrameIndex));
        }
        if(multiViewRenderPass.frames.get(currentFrameIndex).shouldUpdateIndirectCommandsBuffer) {
            updateIndirectCommandBuffer(renderables, multiViewRenderPass.frames.get(currentFrameIndex));
        }

        shouldUpdateGPUSceneBuffer = false;
        multiViewRenderPass.frames.get(currentFrameIndex).shouldUpdateCameraBuffer = false;
        multiViewRenderPass.frames.get(currentFrameIndex).shouldUpdateObjectBuffer = false;
        multiViewRenderPass.frames.get(currentFrameIndex).shouldUpdateIndirectCommandsBuffer = false;
    }

    public void renderMultiViewFrame(MultiViewRenderPassFrame curMultiViewFrame, LongBuffer signalSemaphores, List<Renderable> renderables) {

        try(MemoryStack stack = stackPush()) {

            vkWaitForFences(device, stack.longs(curMultiViewFrame.fence), true, UINT64_MAX);
            vkResetFences(device, stack.longs(curMultiViewFrame.fence));

            recordMultiViewCommandBuffer(renderables, multiViewRenderPass.frames.get(currentMultiViewFrameIndex), multiViewRenderPass.frameBuffer, currentMultiViewFrameIndex);

            // Submit rendering commands to GPU
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);

            submitInfo.pSignalSemaphores(signalSemaphores);
            submitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));
            submitInfo.pCommandBuffers(stack.pointers(curMultiViewFrame.commandBuffer));

            int vkResult;
            if(( vkResult = vkQueueSubmit(graphicsQueue, submitInfo, curMultiViewFrame.fence)) != VK_SUCCESS) {
                vkResetFences(device, stack.longs(curMultiViewFrame.fence));
                throw new RuntimeException("Failed to submit draw command buffer: " + vkResult);
            }

        }
    }

    public Integer prepareDisplay(ViewRenderPassFrame curViewFrame) {
        try(var stack = stackPush()) {

            IntBuffer pImageIndex = stack.mallocInt(1);
            int vkResult = vkAcquireNextImageKHR(device, swapChain, UINT64_MAX,
                    curViewFrame.presentCompleteSemaphore, VK_NULL_HANDLE, pImageIndex);

            if(vkResult == VK_ERROR_OUT_OF_DATE_KHR) {
                recreateSwapChain();
                return null;
            } else if(vkResult != VK_SUCCESS) {
                throw new RuntimeException("Cannot get image");
            }

            return pImageIndex.get(0);

        }
    }

    public void drawViewFrame(ViewRenderPassFrame curViewFrame, LongBuffer waitSemaphore, LongBuffer signalSemaphore, int imageIndex, int viewImageToRender) {
        try(MemoryStack stack = stackPush()) {

            vkWaitForFences(device, curViewFrame.pFence(), true, UINT64_MAX);

            recordViewCommandBuffer(curViewFrame, viewRenderPass.frameBuffers.get(imageIndex), viewImageToRender);

            if(viewRenderPass.imagesToFrameMap.containsKey(imageIndex)) {
                vkWaitForFences(device, viewRenderPass.imagesToFrameMap.get(imageIndex).fence(), true, UINT64_MAX);
            }
            viewRenderPass.imagesToFrameMap.put(imageIndex, curViewFrame);
            vkResetFences(device, curViewFrame.pFence());

            // Submit rendering commands to GPU
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);

            submitInfo.waitSemaphoreCount(1);

            submitInfo.pWaitSemaphores(waitSemaphore);
            submitInfo.pSignalSemaphores(signalSemaphore);
            submitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));
            submitInfo.pCommandBuffers(stack.pointers(curViewFrame.commandBuffer));

            int vkResult;
            if((vkResult = vkQueueSubmit(graphicsQueue, submitInfo, curViewFrame.fence())) != VK_SUCCESS) {
                vkResetFences(device, curViewFrame.pFence());
                throw new RuntimeException("Failed to submit draw command buffer: " + vkResult);
            }

        }
    }

    public void draw(List<Renderable> renderables) {

        try (var stack = stackPush()) {
            var curMultiViewFrame = multiViewRenderPass.frames.get(currentMultiViewFrameIndex);

            var viewFrame1 = viewRenderPass.frames.get(0);
            var viewFrame2 = viewRenderPass.frames.get(1);

            performBufferDataUpdates(renderables, currentMultiViewFrameIndex);

            Integer imageIndex1 = prepareDisplay(viewFrame1);
            if (imageIndex1 == null) return;

            renderMultiViewFrame(curMultiViewFrame, stack.longs(curMultiViewFrame.semaphores.get(0), curMultiViewFrame.semaphores.get(1)), renderables);

            drawViewFrame(viewFrame1, stack.longs(curMultiViewFrame.semaphores.get(0)), stack.longs(viewFrame1.renderFinishedSemaphore), imageIndex1, 0);
            submitDisplay(stack.longs(viewFrame1.renderFinishedSemaphore), imageIndex1);

            Integer imageIndex2 = prepareDisplay(viewFrame2);
            if (imageIndex2 == null) return;
            drawViewFrame(viewFrame2, stack.longs(curMultiViewFrame.semaphores.get(1)), stack.longs(viewFrame2.renderFinishedSemaphore), imageIndex2, 1);
            submitDisplay(stack.longs(viewFrame1.presentCompleteSemaphore, viewFrame2.renderFinishedSemaphore), imageIndex2);

            // Only for multiview
            currentMultiViewFrameIndex = (currentMultiViewFrameIndex + 1) % MAX_FRAMES_IN_FLIGHT;
        }
    }

    public void submitDisplay(LongBuffer waitSemaphores, int imageIndex) {

        try (var stack = stackPush()) {

            // Display rendered image to screen
            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack);
            presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);
            presentInfo.pWaitSemaphores(waitSemaphores);
            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(stack.longs(swapChain));
            presentInfo.pImageIndices(stack.ints(imageIndex));

            int vkResult = vkQueuePresentKHR(presentQueue, presentInfo);

            if (vkResult == VK_ERROR_OUT_OF_DATE_KHR || vkResult == VK_SUBOPTIMAL_KHR || framebufferResize) {
                framebufferResize = false;
                recreateSwapChain();
            } else if (vkResult != VK_SUCCESS) {
                throw new RuntimeException("Failed to present swap chain image");
            }

        }

    }

    public void mergeMeshes(List<Renderable> renderables) {
        int totalVertices = 0;
        int totalIndices = 0;

        for(var r: renderables) {
            r.firstVertex = totalVertices;
            r.firstIndex = totalIndices;

            totalVertices += r.mesh.getVertices().size();
            totalIndices += r.mesh.indices.size();
        }

        mergedVertexBuffer = createBufferVMA(vmaAllocator, totalVertices * (3 + 2 + 3) * Float.BYTES,
                VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE, VMA_ALLOCATION_CREATE_DEDICATED_MEMORY_BIT);

        mergedIndexBuffer = createBufferVMA(vmaAllocator, totalIndices * Integer.BYTES,
                VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
                VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE, VMA_ALLOCATION_CREATE_DEDICATED_MEMORY_BIT);

        try (var stack = stackPush()) {

            var vertexCopy = VkBufferCopy.calloc(1, stack);
            var indexCopy = VkBufferCopy.calloc(1, stack);

            Consumer<VkCommandBuffer> copyCmd = cmd -> {
                for(var r: renderables) {
                    vertexCopy.dstOffset(r.firstVertex * (3 + 2 + 3) * Float.BYTES);
                    vertexCopy.size(r.mesh.getVertices().size() * (3 + 2 + 3) * Float.BYTES);
                    vertexCopy.srcOffset(0);
                    vkCmdCopyBuffer(cmd, r.vertexBuffer.buffer, mergedVertexBuffer.buffer, vertexCopy);

                    indexCopy.dstOffset(r.firstIndex * Integer.BYTES);
                    indexCopy.size(r.mesh.indices.size() * Integer.BYTES);
                    indexCopy.srcOffset(0);
                    vkCmdCopyBuffer(cmd, r.indexBuffer.buffer, mergedIndexBuffer.buffer, indexCopy);
                }
            };

            submitImmediateCommand(copyCmd, singleTimeTransferCommandContext);
        }

        deletionQueue.add(() -> vmaDestroyBuffer(vmaAllocator, mergedVertexBuffer.buffer, mergedVertexBuffer.allocation));
        deletionQueue.add(() -> vmaDestroyBuffer(vmaAllocator, mergedIndexBuffer.buffer, mergedIndexBuffer.allocation));
    }

    public void uploadMeshData(Renderable renderable) {
        createIndexBufferForRenderable(renderable);
        createVertexBufferForRenderable(renderable);
    }

    public Long generateTextureDescriptorSet(TextureVK texture) {

        if(texture.fileName == null) {
            return null;
        }

        textureFileToDescriptorSet.computeIfAbsent(texture.fileName, (s) ->
                new DescriptorBuilder(descriptorSetLayoutCache, descriptorAllocator)
                        .bindImage(0,
                                new DescriptorImageInfo(texture.textureSampler, texture.textureImageView, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL),
                                VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, VK_SHADER_STAGE_FRAGMENT_BIT)
                        .build().descriptorSet()
        );

        return textureFileToDescriptorSet.get(texture.fileName);
    }

    public void createIndexBufferForRenderable(Renderable renderable) {
        try (var stack = stackPush()) {

            var bufferSize = Integer.BYTES * renderable.mesh.indices.size();
            var stagingBuffer = createBufferVMA(vmaAllocator,
                    bufferSize,
                    VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VMA_MEMORY_USAGE_AUTO,
                    VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT);

            var data = stack.mallocPointer(1);

            vmaMapMemory(vmaAllocator, stagingBuffer.allocation, data);
            {
                memcpyInt(data.getByteBuffer(0, bufferSize), renderable.mesh.indices);
            }
            vmaUnmapMemory(vmaAllocator, stagingBuffer.allocation);

            renderable.indexBuffer = createBufferVMA(vmaAllocator, bufferSize,
                    VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE, VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT);

            Consumer<VkCommandBuffer> copyCmd = cmd -> {
                var copy = VkBufferCopy.calloc(1, stack);
                copy.dstOffset(0);
                copy.srcOffset(0);
                copy.size(bufferSize);
                vkCmdCopyBuffer(cmd, stagingBuffer.buffer, renderable.indexBuffer.buffer, copy);
            };

            submitImmediateCommand(copyCmd, singleTimeTransferCommandContext);

            vmaDestroyBuffer(vmaAllocator, stagingBuffer.buffer, stagingBuffer.allocation);
        }
    }

    public void createVertexBufferForRenderable(Renderable renderable) {
        try (var stack = stackPush()) {

            var bufferSize = (3 + 2 + 3) * Float.BYTES * renderable.mesh.getVertices().size();
            var stagingBuffer = createBufferVMA(vmaAllocator,
                    bufferSize,
                    VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VMA_MEMORY_USAGE_AUTO,
                    VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT);

            var data = stack.mallocPointer(1);

            vmaMapMemory(vmaAllocator, stagingBuffer.allocation, data);
            {
                memcpy(data.getByteBuffer(0, bufferSize), renderable.mesh);
            }
            vmaUnmapMemory(vmaAllocator, stagingBuffer.allocation);

            renderable.vertexBuffer = createBufferVMA(vmaAllocator, bufferSize,
                    VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE, VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT);


            Consumer<VkCommandBuffer> copyCmd = cmd -> {
                var copy = VkBufferCopy.calloc(1, stack);
                copy.dstOffset(0);
                copy.srcOffset(0);
                copy.size(bufferSize);
                vkCmdCopyBuffer(cmd, stagingBuffer.buffer, renderable.vertexBuffer.buffer, copy);
            };

            submitImmediateCommand(copyCmd, singleTimeTransferCommandContext);

            vmaDestroyBuffer(vmaAllocator, stagingBuffer.buffer, stagingBuffer.allocation);
        }
    }

    public void createGlobalCommandBuffer() {

        try (var stack = stackPush()) {
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.commandPool(globalCommandPool);
            allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandBufferCount(1);

            PointerBuffer pCommandBuffers = stack.mallocPointer(1);
            if(vkAllocateCommandBuffers(device, allocInfo, pCommandBuffers) != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate command buffers");
            }

            globalCommandBuffer = new VkCommandBuffer(pCommandBuffers.get(0), device);
        }
    }

    public void initDescriptorSets() {

        var objectBufferSize = (int)(padUniformBufferSize(GPUObjectData.SIZEOF, minUniformBufferOffsetAlignment)) * MAXOBJECTS;
        var cameraBufferSize = (int)(padUniformBufferSize(GPUCameraData.SIZEOF, minUniformBufferOffsetAlignment)) * multiViewNumLayers;

        for(int i = 0;i < multiViewRenderPass.frames.size(); i++) {

            var multiViewFrame = multiViewRenderPass.frames.get(i);

            // GPU Scene data descriptor set
            var result = new DescriptorBuilder(descriptorSetLayoutCache, descriptorAllocator)
                    .bindBuffer(0,
                            new DescriptorBufferInfo(0, cameraBufferSize, multiViewFrame.cameraBuffer.buffer),
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
                            new DescriptorBufferInfo(0, objectBufferSize, multiViewFrame.objectBuffer.buffer),
                            VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_VERTEX_BIT)
                    .build();

            multiViewFrame.objectDescriptorSet = result.descriptorSet();
            multiViewRenderPass.objectDescriptorSetLayout = result.layout();

            // Descriptor set for image input for the view Render pass
            // attaches to the output imageview from the multiview pass
            result = new DescriptorBuilder(descriptorSetLayoutCache, descriptorAllocator)
                    .bindImage(0,
                            new DescriptorImageInfo(getTextureSampler(1), multiViewRenderPass.colorAttachment.imageView, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL),
                            VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, VK_SHADER_STAGE_FRAGMENT_BIT)
                    .build();

            viewRenderPass.frames.get(i).imageInputDescriptorSet = result.descriptorSet();
            textureSetLayout = result.layout();
        }
    }

    private void prepareCompute() {

        var objectBufferSize = (int)(padUniformBufferSize(GPUObjectData.SIZEOF, minUniformBufferOffsetAlignment)) * MAXOBJECTS;
        var indirectCommandsBufferSize = MAXCOMMANDS * VkDrawIndexedIndirectCommand.SIZEOF;
        var camerabufferSize = (int)(padUniformBufferSize(GPUCameraData.SIZEOF, minUniformBufferOffsetAlignment)) * multiViewNumLayers;

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

            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack);
            fenceInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
            fenceInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);

            // temp pointers to allocate compute semaphores and fences
            LongBuffer pSemaphore = stack.mallocLong(1);
            LongBuffer pFence = stack.mallocLong(1);

            for (var frame : multiViewRenderPass.frames) {

                var result = new DescriptorBuilder(descriptorSetLayoutCache, descriptorAllocator)
                        .bindBuffer(0, new DescriptorBufferInfo(0, objectBufferSize, frame.objectBuffer.buffer),
                                VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT)
                        .bindBuffer(1, new DescriptorBufferInfo(0, indirectCommandsBufferSize, frame.indirectCommandBuffer.buffer),
                                VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT)
                        .bindBuffer(2, new DescriptorBufferInfo(0, camerabufferSize, frame.cameraBuffer.buffer),
                                VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT)
                        .bindBuffer(3, new DescriptorBufferInfo(0, Integer.BYTES, frame.indirectDrawCountBuffer.buffer),
                                VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT)
                        .build();

                multiViewRenderPass.computeDescriptorSetLayout = result.layout();
                frame.computeDescriptorSet = result.descriptorSet();

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

                vkCheck(vkCreateSemaphore(device, semaphoreInfo, null, pSemaphore));
                vkCheck(vkCreateFence(device, fenceInfo, null, pFence));

                frame.computeSemaphore = pSemaphore.get(0);
                frame.computeFence = pFence.get(0);
            }
        }

    }

    private void recreateSwapChain() {

        try(MemoryStack stack = stackPush()) {

            IntBuffer width = stack.ints(0);
            IntBuffer height = stack.ints(0);

            while(width.get(0) == 0 && height.get(0) == 0) {
                glfwGetFramebufferSize(display.window, width, height);
                glfwWaitEvents();
            }
        }

        vkDeviceWaitIdle(device);

        cleanupSwapChain();

        createSwapChainObjects();
    }

    private void createSwapChainObjects() {
        createSwapChain();
        createSwapChainImageViews();
        createRenderPasses();
        createViewGraphicsPipeline();
        createMultiViewGraphicsPipeline();
        createMultiViewColorAttachment();
        createMultiViewDepthAttachment();
        createFramebuffers();
        updateViewRenderPassImageInputDescriptorSet();

        createGlobalCommandBuffer();
        recreateFrameCommandBuffers();
    }

    public void updateViewRenderPassImageInputDescriptorSet() {
        try (var stack = stackPush()) {
            for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {

                //information about the buffer we want to point at in the descriptor
                var imageBufferInfo = VkDescriptorImageInfo.calloc(1, stack);
                imageBufferInfo.sampler(getTextureSampler(1));
                imageBufferInfo.imageView(multiViewRenderPass.colorAttachment.imageView);
                imageBufferInfo.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

                VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(1, stack);
                var textureWrite =
                        createWriteDescriptorSet(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                                viewRenderPass.frames.get(i).imageInputDescriptorSet,
                                imageBufferInfo,
                                0, stack);
                descriptorWrites.put(0, textureWrite);

                vkUpdateDescriptorSets(device, descriptorWrites, null);
            }
        }
    }

    private void cleanupSwapChain() {

        viewRenderPass.frameBuffers.forEach(fb -> vkDestroyFramebuffer(device, fb, null));
        vkDestroyFramebuffer(device, multiViewRenderPass.frameBuffer, null);

        vkFreeCommandBuffers(device, globalCommandPool, VulkanUtilities.asPointerBuffer(List.of(new VkCommandBuffer[]{globalCommandBuffer})));

        vkDestroyPipeline(device, multiViewGraphicsPipeline, null);
        vkDestroyPipeline(device, viewGraphicsPipeline, null);

        vkDestroyPipelineLayout(device, multiViewPipelineLayout, null);
        vkDestroyPipelineLayout(device, viewPipelineLayout, null);

        vkDestroyRenderPass(device, multiViewRenderPass.renderPass, null);
        vkDestroyRenderPass(device, viewRenderPass.renderPass, null);

        viewRenderPass.swapChainAttachments.forEach(attachment -> vkDestroyImageView(device, attachment.swapChainImageView, null));

        vkDestroyImageView(device, multiViewRenderPass.depthAttachment.imageView, null);
        vmaDestroyImage(vmaAllocator, multiViewRenderPass.depthAttachment.allocatedImage.image, multiViewRenderPass.depthAttachment.allocatedImage.allocation);

        vkDestroyImageView(device, multiViewRenderPass.colorAttachment.imageView, null);
        vmaDestroyImage(vmaAllocator, multiViewRenderPass.colorAttachment.allocatedImage.image, multiViewRenderPass.colorAttachment.allocatedImage.allocation);

        vkDestroySwapchainKHR(device, swapChain, null);
    }

    public void createMultiViewDepthAttachment() {
        try(MemoryStack stack = stackPush()) {
            var depthAttachment = new FrameBufferAttachment();

            int depthFormat = findDepthFormat();
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

    public void createSwapChain() {

        try(MemoryStack stack = stackPush()) {

            SwapChainSupportDetails swapChainSupport = querySwapChainSupport(physicalDevice, surface, stack);

            VkSurfaceFormatKHR surfaceFormat = chooseSwapSurfaceFormat(swapChainSupport.formats());
            int presentMode = chooseSwapPresentMode(swapChainSupport.presentModes());
            VkExtent2D extent = chooseSwapExtent(swapChainSupport.capabilities(), display.window);

            IntBuffer imageCount = stack.ints(swapChainSupport.capabilities().minImageCount() + 1);

            if(swapChainSupport.capabilities().maxImageCount() > 0 && imageCount.get(0) > swapChainSupport.capabilities().maxImageCount()) {
                imageCount.put(0, swapChainSupport.capabilities().maxImageCount());
            }

            VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.calloc(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
            createInfo.surface(surface);
            // Image settings
            createInfo.minImageCount(imageCount.get(0));
            createInfo.imageFormat(surfaceFormat.format());
            createInfo.imageColorSpace(surfaceFormat.colorSpace());
            createInfo.imageExtent(extent);
            createInfo.imageArrayLayers(1);
            createInfo.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);

            if(!queueFamilyIndices.graphicsFamily.equals(queueFamilyIndices.presentFamily)) {
                createInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT);
                createInfo.pQueueFamilyIndices(stack.ints(queueFamilyIndices.graphicsFamily, queueFamilyIndices.presentFamily));
            } else {
                createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
            }

            createInfo.preTransform(swapChainSupport.capabilities().currentTransform());
            createInfo.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
            createInfo.presentMode(presentMode);
            createInfo.clipped(true);

            createInfo.oldSwapchain(VK_NULL_HANDLE);

            LongBuffer pSwapChain = stack.longs(VK_NULL_HANDLE);

            if(vkCreateSwapchainKHR(device, createInfo, null, pSwapChain) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create swap chain");
            }

            swapChain = pSwapChain.get(0);

            vkGetSwapchainImagesKHR(device, swapChain, imageCount, null);

            LongBuffer pSwapchainImages = stack.mallocLong(imageCount.get(0));

            vkGetSwapchainImagesKHR(device, swapChain, imageCount, pSwapchainImages);

            viewRenderPass.swapChainAttachments = new ArrayList<>(imageCount.get(0));

            for(int i = 0;i < pSwapchainImages.capacity();i++) {
                var swapImage = new SwapChainAttachment();
                swapImage.swapChainImage = pSwapchainImages.get(i);
                viewRenderPass.swapChainAttachments.add(swapImage);
            }

            swapChainImageFormat = surfaceFormat.format();
            swapChainExtent = VkExtent2D.create().set(extent);
        }
    }

    public void createSwapChainImageViews() {
        if(viewRenderPass.swapChainAttachments == null) viewRenderPass.swapChainAttachments = new ArrayList<>();

        try (var stack = stackPush()) {

            for (var swapChainImageAttachment : viewRenderPass.swapChainAttachments) {
                var viewInfo =
                        createImageViewCreateInfo(
                                swapChainImageFormat,
                                swapChainImageAttachment.swapChainImage,
                                VK_IMAGE_ASPECT_COLOR_BIT,
                                1,
                                1,
                                VK_IMAGE_VIEW_TYPE_2D,
                                stack
                        );
                swapChainImageAttachment.swapChainImageView = createImageView(viewInfo, device);
            }

        }
    }

    public void createRenderPasses() {
        createViewRenderPass();
        createMultiViewRenderPass();
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
            depthAttachment.format(findDepthFormat());
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
        }
    }

    public void createViewRenderPass() {

        try(MemoryStack stack = stackPush()) {

            VkAttachmentDescription.Buffer attachments;
            VkAttachmentReference.Buffer attachmentRefs;

            attachments = VkAttachmentDescription.calloc(1, stack);
            attachmentRefs = VkAttachmentReference.calloc(1, stack);

            // Color attachment
            var colorAttachment = attachments.get(0);
            colorAttachment.format(swapChainImageFormat);
            colorAttachment.samples(numOfSamples);
            colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
            colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
            colorAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
            colorAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            colorAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            colorAttachment.finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

            var colorAttachmentRef = attachmentRefs.get(0);
            colorAttachmentRef.attachment(0);
            colorAttachmentRef.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1, stack);
            subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);
            subpass.colorAttachmentCount(1);
            subpass.pColorAttachments(VkAttachmentReference.calloc(1, stack).put(0, colorAttachmentRef));

            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack);
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO);
            renderPassInfo.pAttachments(attachments);
            renderPassInfo.pSubpasses(subpass);

            LongBuffer pRenderPass = stack.mallocLong(1);

            if(vkCreateRenderPass(device, renderPassInfo, null, pRenderPass) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create render pass");
            }

            viewRenderPass.renderPass = pRenderPass.get(0);
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
        builder.viewport = new PipelineBuilder.ViewPort(swapChainExtent.width(), swapChainExtent.height());
        builder.scissor = new PipelineBuilder.Scissor(swapChainExtent);

        builder.shaderStages.add(new PipelineBuilder.ShaderStageCreateInfo("shaders/multiview.vert", VK_SHADER_STAGE_VERTEX_BIT));
        builder.shaderStages.add(new PipelineBuilder.ShaderStageCreateInfo("shaders/multiview.frag", VK_SHADER_STAGE_FRAGMENT_BIT));
        builder.descriptorSetLayouts = new long[]{multiViewRenderPass.cameraAndSceneDescriptorSetLayout, multiViewRenderPass.objectDescriptorSetLayout, textureSetLayout};

        var pipeLineCreateResults = builder.build(device, multiViewRenderPass.renderPass);
        multiViewPipelineLayout = pipeLineCreateResults.pipelineLayout();
        multiViewGraphicsPipeline = pipeLineCreateResults.pipeline();

    }

    public void createViewGraphicsPipeline() {
        var builder = new PipelineBuilder();
        builder.viewport = new PipelineBuilder.ViewPort(swapChainExtent.width(), swapChainExtent.height());
        builder.scissor = new PipelineBuilder.Scissor(swapChainExtent);

        builder.shaderStages.add(new PipelineBuilder.ShaderStageCreateInfo("shaders/ActiveShutterView.vert", VK_SHADER_STAGE_VERTEX_BIT));
        builder.shaderStages.add(new PipelineBuilder.ShaderStageCreateInfo("shaders/ActiveShutterView.frag", VK_SHADER_STAGE_FRAGMENT_BIT));
        builder.descriptorSetLayouts = new long[]{textureSetLayout};
        builder.pushConstant = new PipelineBuilder.PushConstant(0, Float.BYTES, VK_SHADER_STAGE_FRAGMENT_BIT);
        builder.depthStencil = new PipelineBuilder.PipelineDepthStencilStateCreateInfo(false, false, VK_COMPARE_OP_LESS_OR_EQUAL, false, false);
        builder.rasterizer = new PipelineBuilder.PipelineRasterizationStateCreateInfo(VK_CULL_MODE_FRONT_BIT, VK_FRONT_FACE_COUNTER_CLOCKWISE);

        var pipeLineCreateResults = builder.build(device, viewRenderPass.renderPass);
        viewPipelineLayout = pipeLineCreateResults.pipelineLayout();
        viewGraphicsPipeline = pipeLineCreateResults.pipeline();
    }

    public void createFramebuffers() {

        try(MemoryStack stack = stackPush()) {

            viewRenderPass.frameBuffers = new ArrayList<>();

            var attachments = stack.longs(VK_NULL_HANDLE);

            LongBuffer pFramebuffer = stack.mallocLong(1);

            // Create one framebuffer per swapchain image available
            VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.calloc(stack);
            framebufferInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
            framebufferInfo.renderPass(viewRenderPass.renderPass);
            framebufferInfo.width(swapChainExtent.width());
            framebufferInfo.height(swapChainExtent.height());
            framebufferInfo.layers(1);

            for(int i = 0; i < viewRenderPass.swapChainAttachments.size(); i++) {

                attachments.put(0, viewRenderPass.swapChainAttachments.get(i).swapChainImageView);
                framebufferInfo.pAttachments(attachments);

                if(vkCreateFramebuffer(device, framebufferInfo, null, pFramebuffer) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create framebuffer");
                }

                viewRenderPass.frameBuffers.add(pFramebuffer.get(0));
            }

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

    public void createGlobalCommandPool() {

        try(MemoryStack stack = stackPush()) {

            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
            poolInfo.queueFamilyIndex(queueFamilyIndices.graphicsFamily);
            poolInfo.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);

            LongBuffer pCommandPool = stack.mallocLong(1);

            if (vkCreateCommandPool(device, poolInfo, null, pCommandPool) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create command pool");
            }

            globalCommandPool = pCommandPool.get(0);
            deletionQueue.add(() -> vkDestroyCommandPool(device, globalCommandPool, null));

        }
    }

    public void createLogicalDevice() {
        try(MemoryStack stack = stackPush()) {

            QueueFamilyIndices indices = VulkanUtilities.findQueueFamilies(physicalDevice, surface);

            queueFamilyIndices = indices;
            int[] uniqueQueueFamilies = indices.unique();

            VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.calloc(uniqueQueueFamilies.length, stack);

            for(int i = 0;i < uniqueQueueFamilies.length;i++) {
                VkDeviceQueueCreateInfo queueCreateInfo = queueCreateInfos.get(i);
                queueCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
                queueCreateInfo.queueFamilyIndex(uniqueQueueFamilies[i]);
                queueCreateInfo.pQueuePriorities(stack.floats(1.0f));
            }

            var deviceFeatures = VkPhysicalDeviceFeatures.calloc(stack);
            deviceFeatures.samplerAnisotropy(true);
            if(msaaEnabled) {
                deviceFeatures.sampleRateShading(true);
            }

            var vkPhysicalDeviceVulkan11Features = VkPhysicalDeviceVulkan11Features.calloc(stack);
            vkPhysicalDeviceVulkan11Features.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_1_FEATURES);
            vkPhysicalDeviceVulkan11Features.shaderDrawParameters(true);

            vkPhysicalDeviceVulkan11Features.multiview(true);

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
            createInfo.pQueueCreateInfos(queueCreateInfos);
            createInfo.pNext(vkPhysicalDeviceVulkan11Features);
            // queueCreateInfoCount is automatically set
            createInfo.pEnabledFeatures(deviceFeatures);

            createInfo.ppEnabledExtensionNames(VulkanUtilities.asPointerBuffer(DEVICE_EXTENSIONS));
            if(ENABLE_VALIDATION_LAYERS) {
                createInfo.ppEnabledLayerNames(VulkanUtilities.asPointerBuffer(VALIDATION_LAYERS));
            }

            PointerBuffer pDevice = stack.pointers(VK_NULL_HANDLE);

            if(vkCreateDevice(physicalDevice, createInfo, null, pDevice) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create logical device");
            }

            device = new VkDevice(pDevice.get(0), physicalDevice, createInfo);
            deletionQueue.add(() -> vkDestroyDevice(device, null));

            PointerBuffer pQueue = stack.pointers(VK_NULL_HANDLE);

            vkGetDeviceQueue(device, indices.graphicsFamily, 0, pQueue);
            graphicsQueue = new VkQueue(pQueue.get(0), device);

            vkGetDeviceQueue(device, indices.presentFamily, 0, pQueue);
            presentQueue = new VkQueue(pQueue.get(0), device);

            vkGetDeviceQueue(device, indices.computeFamily, 0, pQueue);
            computeQueue = new VkQueue(pQueue.get(0), device);

            vkGetDeviceQueue(device, indices.transferFamily, 0, pQueue);
            transferQueue = new VkQueue(pQueue.get(0), device);
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

    public void updateCameraGPUDataInMemory(int currentFrame) {
        var alignment = (int)(padUniformBufferSize(GPUCameraData.SIZEOF, minUniformBufferOffsetAlignment));
        int bufferSize = (int) (padUniformBufferSize(GPUCameraData.SIZEOF, minUniformBufferOffsetAlignment) * multiViewNumLayers);

        var bw = new BufferWriter(vmaAllocator, multiViewRenderPass.frames.get(currentFrame).cameraBuffer, alignment, bufferSize);
        bw.mapBuffer();
        GPUCameraData.memcpy(bw.buffer, gpuCameraDataLeft);
        bw.setPosition(1);
        GPUCameraData.memcpy(bw.buffer, gpuCameraDataRight);
        bw.unmapBuffer();
    }

    public void updateSceneGPUDataInMemory(int currentFrame) {

        var alignment = (int)padUniformBufferSize(GPUSceneData.SIZEOF, minUniformBufferOffsetAlignment);
        int bufferSize = (int) (padUniformBufferSize(GPUSceneData.SIZEOF, minUniformBufferOffsetAlignment) * MAX_FRAMES_IN_FLIGHT);

        var bw = new BufferWriter(vmaAllocator, gpuSceneBuffer, alignment, bufferSize);
        bw.mapBuffer();
        bw.setPosition(currentFrame);
        GPUSceneData.memcpy(bw.buffer, gpuSceneData);
        bw.unmapBuffer();
    }

    public void updateObjectBufferDataInMemory(List<Renderable> renderables, MultiViewRenderPassFrame frame) {

        var alignmentSize = (int)(padUniformBufferSize(RenderingEngineVulkan.GPUObjectData.SIZEOF, minUniformBufferOffsetAlignment));
        int bufferSize = alignmentSize * renderables.size();

        var bw = new BufferWriter(vmaAllocator, frame.objectBuffer, alignmentSize, bufferSize);
        bw.mapBuffer();
        for(int i = 0; i < renderables.size(); i++) {
            bw.setPosition(i);
            bw.put(renderables.get(i).model.objectToWorldMatrix);
        }
        bw.unmapBuffer();
    }

    public void updateIndirectCommandBuffer(List<Renderable> renderables, MultiViewRenderPassFrame frame) {

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

    public void recreateFrameCommandBuffers() {
        try (MemoryStack stack = stackPush()) {
            for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {

                // Create command buffer
                VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
                allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
                allocInfo.commandPool(globalCommandPool);
                allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
                allocInfo.commandBufferCount(1);

                PointerBuffer pCommandBuffers = stack.mallocPointer(1);
                if(vkAllocateCommandBuffers(device, allocInfo, pCommandBuffers) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to allocate command buffers");
                }

                var commandBuffer = new VkCommandBuffer(pCommandBuffers.get(0), device);
                viewRenderPass.frames.get(i).commandBuffer = commandBuffer;

                // Create command buffer for MultiView Render pass
                allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
                allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
                allocInfo.commandPool(globalCommandPool);
                allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
                allocInfo.commandBufferCount(1);

                pCommandBuffers = stack.mallocPointer(1);
                if(vkAllocateCommandBuffers(device, allocInfo, pCommandBuffers) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to allocate command buffers");
                }

                commandBuffer = new VkCommandBuffer(pCommandBuffers.get(0), device);
                multiViewRenderPass.frames.get(i).commandBuffer = commandBuffer;
            }
        }
    }

    public void initializeFrames() {
        viewRenderPass.frames = new ArrayList<>(MAX_FRAMES_IN_FLIGHT);
        multiViewRenderPass.frames = new ArrayList<>(MAX_FRAMES_IN_FLIGHT);

        for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
            var frame = new ViewRenderPassFrame();
            viewRenderPass.frames.add(frame);

            var newFrame = new MultiViewRenderPassFrame();
            newFrame.vmaAllocator = vmaAllocator;
            multiViewRenderPass.frames.add(newFrame);
        }
    }

    public void createFrameCommandPoolsAndCommandBuffers() {

        try (var stack = stackPush()) {

            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
            poolInfo.queueFamilyIndex(queueFamilyIndices.graphicsFamily);
            poolInfo.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);

            for (int i = 0; i < viewRenderPass.frames.size(); i++) {

                // Create command pool and command buffer for ViewRenderPass
                LongBuffer pCommandPool = stack.mallocLong(1);

                if (vkCreateCommandPool(device, poolInfo, null, pCommandPool) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create command pool");
                }
                var commandPool = pCommandPool.get(0);

                // Create command buffer
                VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
                allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
                allocInfo.commandPool(commandPool);
                allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
                allocInfo.commandBufferCount(1);

                PointerBuffer pCommandBuffers = stack.mallocPointer(1);
                if (vkAllocateCommandBuffers(device, allocInfo, pCommandBuffers) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to allocate command buffers");
                }

                var commandBuffer = new VkCommandBuffer(pCommandBuffers.get(0), device);

                viewRenderPass.frames.get(i).commandPool = commandPool;
                viewRenderPass.frames.get(i).commandBuffer = commandBuffer;

                // Create command pool and command buffer for MultiView RenderPass
                if (vkCreateCommandPool(device, poolInfo, null, pCommandPool) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create command pool");
                }
                commandPool = pCommandPool.get(0);

                // Create command buffer
                allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
                allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
                allocInfo.commandPool(commandPool);
                allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
                allocInfo.commandBufferCount(1);

                pCommandBuffers = stack.mallocPointer(1);
                if (vkAllocateCommandBuffers(device, allocInfo, pCommandBuffers) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to allocate command buffers");
                }

                commandBuffer = new VkCommandBuffer(pCommandBuffers.get(0), device);

                multiViewRenderPass.frames.get(i).commandPool = commandPool;
                multiViewRenderPass.frames.get(i).commandBuffer = commandBuffer;

            }
        }
    }

    public void initSyncObjects() {

        viewRenderPass.imagesToFrameMap = new HashMap<>(MAX_FRAMES_IN_FLIGHT);

        try (MemoryStack stack = stackPush()) {

            VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack);
            semaphoreInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack);
            fenceInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
            fenceInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);

            LongBuffer pImageAvailableSemaphore = stack.mallocLong(1);
            LongBuffer pRenderFinishedSemaphore = stack.mallocLong(1);
            LongBuffer pFence = stack.mallocLong(1);

            // For view renderPass
            for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {

                if (vkCreateSemaphore(device, semaphoreInfo, null, pImageAvailableSemaphore) != VK_SUCCESS
                        || vkCreateSemaphore(device, semaphoreInfo, null, pRenderFinishedSemaphore) != VK_SUCCESS
                        || vkCreateFence(device, fenceInfo, null, pFence) != VK_SUCCESS) {

                    throw new RuntimeException("Failed to create synchronization objects for the frame " + i);
                }

                viewRenderPass.frames.get(i).presentCompleteSemaphore = pImageAvailableSemaphore.get(0);
                viewRenderPass.frames.get(i).renderFinishedSemaphore = pRenderFinishedSemaphore.get(0);
                viewRenderPass.frames.get(i).fence = pFence.get(0);

            }

            // For multiview renderpass
            LongBuffer sempahore1 = stack.mallocLong(1);
            LongBuffer sempahore2 = stack.mallocLong(1);

            for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {

                vkCheck(vkCreateSemaphore(device, semaphoreInfo, null, sempahore1));
                vkCheck(vkCreateSemaphore(device, semaphoreInfo, null, sempahore2));
                vkCheck(vkCreateFence(device, fenceInfo, null, pFence));

                multiViewRenderPass.frames.get(i).semaphores.add(sempahore1.get(0));
                multiViewRenderPass.frames.get(i).semaphores.add(sempahore2.get(0));
                multiViewRenderPass.frames.get(i).fence = pFence.get(0);
            }

            // Create fence and commandPool/buffer for immediate Graphics context
            singleTimeGraphicsCommandContext = new SingleTimeCommandContext();
            singleTimeGraphicsCommandContext.fence = createFence(VkFenceCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO));
            singleTimeGraphicsCommandContext.commandPool =  createCommandPool(device,
                    createCommandPoolCreateInfo(queueFamilyIndices.graphicsFamily, 0, stack), stack);
            singleTimeGraphicsCommandContext.queue = graphicsQueue;
            var cmdAllocInfo = createCommandBufferAllocateInfo(singleTimeGraphicsCommandContext.commandPool, 1, VK_COMMAND_BUFFER_LEVEL_PRIMARY, stack);
            singleTimeGraphicsCommandContext.commandBuffer = createCommandBuffer(device, cmdAllocInfo, stack);

            // Create fence and commandPool/buffer for immediate trasnfer context
            singleTimeTransferCommandContext = new SingleTimeCommandContext();
            singleTimeTransferCommandContext.fence = createFence(VkFenceCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO));
            singleTimeTransferCommandContext.commandPool =  createCommandPool(device,
                    createCommandPoolCreateInfo(queueFamilyIndices.transferFamily, 0, stack), stack);
            singleTimeTransferCommandContext.queue = transferQueue;
            cmdAllocInfo = createCommandBufferAllocateInfo(singleTimeTransferCommandContext.commandPool, 1, VK_COMMAND_BUFFER_LEVEL_PRIMARY, stack);
            singleTimeTransferCommandContext.commandBuffer = createCommandBuffer(device, cmdAllocInfo, stack);

            deletionQueue.add(() -> singleTimeGraphicsCommandContext.cleanUp(device));
            deletionQueue.add(() -> singleTimeTransferCommandContext.cleanUp(device));
        }
    }

    @Override
    public void cleanUp() {

        // Wait for the device to complete all operations before release resources
        vkDeviceWaitIdle(device);

        for(int i = deletionQueue.size()-1; i >= 0; i--) {
            deletionQueue.get(i).run();
        }
    }
    public static class GPUCameraData {

        public static final int SIZEOF = 3 * 16 * Float.BYTES;
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
        public static final int SIZEOF = Float.BYTES * 4 * 4;
        public Matrix modelMatrix;
        public static void memcpy(ByteBuffer buffer, GPUObjectData data) {
            data.modelMatrix.setValuesToBuffer(buffer);
        }
    }

    public class MultiViewRenderPassFrame {

        public List<Long> semaphores = new ArrayList<>();
        public long fence;
        public long commandPool;

        public long computeCommandPool;
        public VkCommandBuffer computeCommandBuffer;
        public long computeFence;
        public long computeSemaphore;
        public long computeDescriptorSet;
        public VkCommandBuffer commandBuffer;
        public AllocatedBuffer cameraBuffer;
        public AllocatedBuffer objectBuffer;
        public AllocatedBuffer indirectCommandBuffer;
        public AllocatedBuffer indirectDrawCountBuffer;

        // Global Descriptor set contains the camera data and other scene parameters
        public long cameraAndSceneDescriptorSet;

        // This contains the object transformation matrices
        public long objectDescriptorSet;

        public long vmaAllocator;

        public boolean shouldUpdateIndirectCommandsBuffer = true;
        public boolean shouldUpdateObjectBuffer = true;
        public boolean shouldUpdateCameraBuffer = true;

        public void cleanUp() {
            semaphores.forEach(s -> vkDestroySemaphore(device, s, null));
            vkDestroySemaphore(device, computeSemaphore, null);
            vkDestroyFence(device, fence, null);
            vkDestroyFence(device, computeFence, null);
            vkDestroyCommandPool(device, commandPool, null);
            vkDestroyCommandPool(device, computeCommandPool, null);
            vmaDestroyBuffer(vmaAllocator, cameraBuffer.buffer, cameraBuffer.allocation);
            vmaDestroyBuffer(vmaAllocator, objectBuffer.buffer, objectBuffer.allocation);
            vmaDestroyBuffer(vmaAllocator, indirectCommandBuffer.buffer, indirectCommandBuffer.allocation);
            vmaDestroyBuffer(vmaAllocator, indirectDrawCountBuffer.buffer, indirectDrawCountBuffer.allocation);
        }
    }

    public class ViewRenderPassFrame {

        public long presentCompleteSemaphore;
        public long renderFinishedSemaphore;
        public long fence;
        public long commandPool;
        public VkCommandBuffer commandBuffer;
        public long imageInputDescriptorSet;

        public void cleanUp() {
            vkDestroySemaphore(device, renderFinishedSemaphore, null);
            vkDestroySemaphore(device, presentCompleteSemaphore, null);
            vkDestroyFence(device, fence(), null);
            vkDestroyCommandPool(device, commandPool, null);
        }

        public long fence() {
            return fence;
        }
        public LongBuffer pFence() {
            return stackGet().longs(fence);
        }

    }

}
