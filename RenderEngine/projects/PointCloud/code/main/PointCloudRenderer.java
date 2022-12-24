package main;

import Kurama.Math.Vector;
import Kurama.Mesh.Mesh;
import Kurama.Mesh.Meshlet;
import Kurama.Vulkan.*;
import Kurama.game.Game;
import org.lwjgl.vulkan.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static Kurama.Vulkan.VulkanUtilities.*;
import static Kurama.utils.Logger.log;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.EXTMeshShader.*;
import static org.lwjgl.vulkan.KHRDynamicRendering.*;
import static org.lwjgl.vulkan.KHRDynamicRendering.vkCmdEndRenderingKHR;
import static org.lwjgl.vulkan.KHRShaderFloatControls.VK_KHR_SHADER_FLOAT_CONTROLS_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSpirv14.VK_KHR_SPIRV_1_4_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.KHRSynchronization2.VK_IMAGE_LAYOUT_ATTACHMENT_OPTIMAL_KHR;
import static org.lwjgl.vulkan.NVMeshShader.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MESH_SHADER_FEATURES_NV;
import static org.lwjgl.vulkan.VK10.*;

public class PointCloudRenderer extends VulkanRendererBase {

    public boolean windowResized = false;
    public long descriptorSetLayout;

    public PointCloudController controller;
    public int MAXOBJECTS = 10000;
    public int MAXVERTICES = 1000000;
    public int MAXMESHLETS = 1000000;
    public int GPUObjectData_SIZEOF = Float.BYTES * (16 + 4);
    public int VERTEX_SIZE = Float.BYTES * 8;
    public int MESHLETSIZE = Float.BYTES * 8;
    public List<Frame> frames = new ArrayList<>();
    public List<Meshlet> meshlets = new ArrayList<>();
    public Map<Mesh.VERTATTRIB, List<Vector>> globalVertAttribs = new HashMap<>();
    public List<Integer> meshletVertexIndexBuffer = new ArrayList<>();
    public List<Integer> meshletLocalIndexBuffer = new ArrayList<>();

    public class Frame {
        public AllocatedBuffer cameraBuffer;
        public AllocatedBuffer objectBuffer;
        public AllocatedBuffer vertexBuffer;
        public AllocatedBuffer meshletVertexBuffer;
        public AllocatedBuffer meshletVertexLocalIndexBuffer;
        public AllocatedBuffer meshletDescBuffer;
        public long descriptorSet;
    }

    public PointCloudRenderer(Game game) {
        super(game);

//        DEVICE_EXTENSIONS.add(VK_KHR_GET_PHYSICAL_DEVICE_PROPERTIES_2_EXTENSION_NAME);
        DEVICE_EXTENSIONS.add(VK_EXT_MESH_SHADER_EXTENSION_NAME);
        DEVICE_EXTENSIONS.add(VK_KHR_SPIRV_1_4_EXTENSION_NAME);
        DEVICE_EXTENSIONS.add(VK_KHR_SHADER_FLOAT_CONTROLS_EXTENSION_NAME);

        var meshShaderFeatures = VkPhysicalDeviceMeshShaderFeaturesEXT.calloc();
        meshShaderFeatures.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MESH_SHADER_FEATURES_NV);
        meshShaderFeatures.meshShader(true);
        meshShaderFeatures.taskShader(true);
        createInfo.pNext(meshShaderFeatures);

        this.controller = (PointCloudController) game;
    }

    @Override
    public void initRenderer() {

        for(int i = 0; i < drawCmds.size(); i++) {
            frames.add(new Frame());
        }

        initBuffers();
        initDescriptorSets();
        createDepthAttachment();
        initPipelines();
        recordCommandBuffers();
    }

    public void recordCommandBuffers() {
        try (var stack = stackPush()) {

            for(int i = 0; i < drawCmds.size(); i++) {

                var commandBuffer = drawCmds.get(i);

                vkCheck(vkResetCommandBuffer(commandBuffer, 0), "Failed to begin recording command buffer");

                VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
                beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

                vkCheck(vkBeginCommandBuffer(commandBuffer, beginInfo), "Failed to begin recording command buffer");

                // For dynamic Rendering
                insertImageMemoryBarrier(commandBuffer, swapChainAttachments.get(i).swapChainImage,
                        0, VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT,
                        VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                        VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
                        VkImageSubresourceRange.calloc(stack)
                                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                                .baseMipLevel(0)
                                .levelCount(1)
                                .layerCount(1)
                                .baseArrayLayer(0));

                insertImageMemoryBarrier(commandBuffer, depthAttachment.allocatedImage.image,
                        0, VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT,
                        VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
                        VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT | VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT,
                        VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT | VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT,
                        VkImageSubresourceRange.calloc(stack)
                                .aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT | VK_IMAGE_ASPECT_STENCIL_BIT)
                                .baseMipLevel(0)
                                .levelCount(1)
                                .layerCount(1)
                                .baseArrayLayer(0));

                var colorAttachment = VkRenderingAttachmentInfoKHR.calloc(1, stack);
                colorAttachment.sType(VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO_KHR);
                colorAttachment.imageView(swapChainAttachments.get(i).swapChainImageView);
                colorAttachment.imageLayout(VK_IMAGE_LAYOUT_ATTACHMENT_OPTIMAL_KHR);
                colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
                colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
                colorAttachment.clearValue().color(VkClearValue.calloc(stack).color().float32(stack.floats(0.0f, 0.0f, 0.0f, 0.0f)));

                var depthStencilAttachment = VkRenderingAttachmentInfoKHR.calloc(1, stack);
                depthStencilAttachment.sType(VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO_KHR);
                depthStencilAttachment.imageView(depthAttachment.imageView);
                depthStencilAttachment.imageLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
                depthStencilAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
                depthStencilAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
                depthStencilAttachment.clearValue().depthStencil(VkClearDepthStencilValue.calloc(stack).set(1f, 0));

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

                    vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                            pipelineLayout, 0,
                            stack.longs(frames.get(i).descriptorSet), null);

                    vkCmdDrawMeshTasksEXT(commandBuffer, 1, 1, 1);

                }

                vkCmdEndRenderingKHR(commandBuffer);

                insertImageMemoryBarrier(commandBuffer, swapChainAttachments.get(i).swapChainImage,
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
    }

    @Override
    public void render() {
        prepareFrame();
        drawFrame();
        submitFrame();
    }

    @Override
    public void swapChainRecreatedEvent() {
        recordCommandBuffers();
    }

    @Override
    public void meshesMergedEvent() {
        updateObjectBufferDataInMemory(renderables);
        updateVertexAndIndexBuffers();
        updateMeshletInfoBuffer();
    }

    @Override
    public void cameraUpdatedEvent() {
        updateCameraBuffer();
    }

    // TODO: This must be based on updating portions of the list instead of entire portions
    public void updateMeshletInfoBuffer() {
        for(var frame: frames) {
            // Update meshlets desc buffer
            var bw = new BufferWriter(vmaAllocator, frame.meshletDescBuffer, MESHLETSIZE, MESHLETSIZE * MAXMESHLETS);
            bw.mapBuffer();
            for(int i = 0; i < meshlets.size(); i++) {
                bw.setPosition(i);
                bw.put(meshlets.get(i).primitiveCount);
                bw.put(meshlets.get(i).vertexCount);
                bw.put(meshlets.get(i).indexBegin);
                bw.put(meshlets.get(i).vertexBegin);
                bw.put(meshlets.get(i).pos);
                bw.put(meshlets.get(i).boundRadius);
            }
            bw.unmapBuffer();
        }
    }

    public void updateVertexAndIndexBuffers() {

        try (var stack = stackPush()) {

            var meshletVerticesStagingBuffer = VulkanUtilities.createBufferVMA(vmaAllocator,
                    Integer.BYTES * MAXVERTICES,
                    VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VMA_MEMORY_USAGE_AUTO,
                    VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT);

            var  meshletIndicesStagingBuffer = VulkanUtilities.createBufferVMA(vmaAllocator,
                    MAXVERTICES * Integer.BYTES,
                    VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VMA_MEMORY_USAGE_AUTO,
                    VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT);

            var  verticesStagingBuffer = VulkanUtilities.createBufferVMA(vmaAllocator,
                    MAXVERTICES * VERTEX_SIZE,
                    VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VMA_MEMORY_USAGE_AUTO,
                    VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT);

        }

    }

    public void updateObjectBufferDataInMemory(List<Renderable> renderables) {
        // TODO: Check whether buffer must be resized

        var alignmentSize = GPUObjectData_SIZEOF;
        int bufferSize = alignmentSize * renderables.size();

        for(var frame: frames) {

            var bw = new BufferWriter(vmaAllocator, frame.objectBuffer, alignmentSize, bufferSize);
            bw.mapBuffer();
            for (int i = 0; i < renderables.size(); i++) {
                var renderable = renderables.get(i);
                if (renderable.isDirty) {
                    bw.setPosition(i);
                    bw.put(renderable.model.objectToWorldMatrix);
                    bw.put(renderable.model.getScale().getNorm() * renderable.mesh.boundingRadius);
                    renderable.isDirty = false;
                }
            }

            bw.unmapBuffer();
        }
    }

    @Override
    public void initDescriptorSets() {
        for(var frame: frames) {
            var results = new DescriptorBuilder(descriptorSetLayoutCache, descriptorAllocator)
                    .bindBuffer(0, new DescriptorBufferInfo(0, VK_WHOLE_SIZE, frame.cameraBuffer.buffer),
                            VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, VK_SHADER_STAGE_MESH_BIT_EXT)
                    .bindBuffer(1, new DescriptorBufferInfo(0, VK_WHOLE_SIZE, frame.objectBuffer.buffer),
                            VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_MESH_BIT_EXT)
                    .bindBuffer(2, new DescriptorBufferInfo(0, VK_WHOLE_SIZE, frame.vertexBuffer.buffer),
                            VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_MESH_BIT_EXT)
                    .build();

            descriptorSetLayout = results.layout();
            frame.descriptorSet = results.descriptorSet();
        }
    }

    @Override
    public void initBuffers() {
        var objectBufferSize = GPUObjectData_SIZEOF * MAXOBJECTS;
        var vertexBufferSize = VERTEX_SIZE * MAXVERTICES;

        for(var frame: frames) {
            frame.cameraBuffer = createBufferVMA(
                    vmaAllocator,
                    Float.BYTES * 3 * 16,
                    VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                    VMA_MEMORY_USAGE_AUTO,
                    VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT |
                            VMA_ALLOCATION_CREATE_HOST_ACCESS_ALLOW_TRANSFER_INSTEAD_BIT);

            frame.objectBuffer = createBufferVMA(
                    vmaAllocator,
                    objectBufferSize,
                    VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
                    VMA_MEMORY_USAGE_AUTO,
                    VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT |
                            VMA_ALLOCATION_CREATE_HOST_ACCESS_ALLOW_TRANSFER_INSTEAD_BIT
            );

            frame.vertexBuffer = VulkanUtilities.createBufferVMA(vmaAllocator,
                    MAXVERTICES * VERTEX_SIZE,
                    VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
                    VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE, VMA_ALLOCATION_CREATE_DEDICATED_MEMORY_BIT
            );

            var meshletVertIndexBuffSize = Integer.BYTES * MAXVERTICES;

            frame.meshletVertexBuffer = VulkanUtilities.createBufferVMA(vmaAllocator, meshletVertIndexBuffSize,
                    VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
                    VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE, VMA_ALLOCATION_CREATE_DEDICATED_MEMORY_BIT);

            frame.meshletVertexLocalIndexBuffer = VulkanUtilities.createBufferVMA(vmaAllocator, meshletVertIndexBuffSize,
                    VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
                    VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE, VMA_ALLOCATION_CREATE_DEDICATED_MEMORY_BIT);

            var meshletDescBufferSize = MAXMESHLETS * MESHLETSIZE;
            frame.meshletDescBuffer = createBufferVMA(
                    vmaAllocator,
                    meshletDescBufferSize,
                    VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
                    VMA_MEMORY_USAGE_AUTO,
                    VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT |
                            VMA_ALLOCATION_CREATE_HOST_ACCESS_ALLOW_TRANSFER_INSTEAD_BIT
            );
        }
    }

    @Override
    public void initRenderPasses() {

    }

    @Override
    public void initPipelines() {
        var builder = new PipelineBuilder(PipelineBuilder.PipelineType.GRAPHICS);

        builder.rasterizer = new PipelineBuilder.PipelineRasterizationStateCreateInfo(VK_CULL_MODE_NONE, VK_FRONT_FACE_CLOCKWISE);
        builder.dynamicStates.add(VK_DYNAMIC_STATE_VIEWPORT);
        builder.dynamicStates.add(VK_DYNAMIC_STATE_SCISSOR);
        builder.colorAttachmentImageFormat = swapChainImageFormat;
        builder.descriptorSetLayouts = new long[]{descriptorSetLayout};

        builder.shaderStages.add(new PipelineBuilder.ShaderStageCreateInfo("shaders/meshshader.task", VK_SHADER_STAGE_TASK_BIT_EXT));
        builder.shaderStages.add(new PipelineBuilder.ShaderStageCreateInfo("shaders/meshshader.mesh", VK_SHADER_STAGE_MESH_BIT_EXT));
        builder.shaderStages.add(new PipelineBuilder.ShaderStageCreateInfo("shaders/meshshader.frag", VK_SHADER_STAGE_FRAGMENT_BIT));

        var pipeLineCreateResults = builder.build(device, null);
        pipelineLayout = pipeLineCreateResults.pipelineLayout();
        graphicsPipeline = pipeLineCreateResults.pipeline();

        deletionQueue.add(() -> vkDestroyPipeline(device, graphicsPipeline, null));
        deletionQueue.add(() -> vkDestroyPipelineLayout(device, pipelineLayout, null));
    }

    public void updateCameraBuffer() {
        var alignment = Float.BYTES * 3 * 16;
        var bufferSize = alignment;

        var projMat = controller.mainCamera.getPerspectiveProjectionMatrix();
        var camViewMat = controller.mainCamera.worldToObject;
        var projView = projMat.matMul(camViewMat);

        for(var frame: frames) {
            var bw = new BufferWriter(vmaAllocator, frame.cameraBuffer, alignment, bufferSize);
            bw.mapBuffer();

            projView.setValuesToBuffer(bw.buffer);
            projMat.setValuesToBuffer(bw.buffer);
            projView.setValuesToBuffer(bw.buffer);

            bw.unmapBuffer();
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
}
