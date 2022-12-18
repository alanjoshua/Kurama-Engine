package main;

import Kurama.Math.Matrix;
import Kurama.Vulkan.*;
import Kurama.game.Game;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static Kurama.Vulkan.VulkanUtilities.*;
import static Kurama.utils.Logger.log;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.EXTMeshShader.*;
import static org.lwjgl.vulkan.KHRDynamicRendering.*;
import static org.lwjgl.vulkan.KHRDynamicRendering.vkCmdEndRenderingKHR;
import static org.lwjgl.vulkan.KHRGetPhysicalDeviceProperties2.VK_KHR_GET_PHYSICAL_DEVICE_PROPERTIES_2_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRShaderFloatControls.VK_KHR_SHADER_FLOAT_CONTROLS_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSpirv14.VK_KHR_SPIRV_1_4_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.KHRSynchronization2.VK_IMAGE_LAYOUT_ATTACHMENT_OPTIMAL_KHR;
import static org.lwjgl.vulkan.NVMeshShader.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MESH_SHADER_FEATURES_NV;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.VK_STRUCTURE_TYPE_TIMELINE_SEMAPHORE_SUBMIT_INFO;

public class PointCloudRenderer extends VulkanRendererBase {

    public boolean windowResized = false;
    public long descriptorSetLayout;
    public long descriptorSet;
    public AllocatedBuffer ubo;
    public PointCloudController controller;

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

                    // Bind color Attachment from previous multiview Renderpass as input
                    vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                            pipelineLayout, 0,
                            stack.longs(descriptorSet), null);

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

    }

    @Override
    public void meshesMergedEvent() {

    }

    @Override
    public void cameraUpdatedEvent() {
        updateUBO();
    }

    @Override
    public void initDescriptorSets() {
        var results = new DescriptorBuilder(descriptorSetLayoutCache, descriptorAllocator)
                .bindBuffer(0, new DescriptorBufferInfo(0, VK_WHOLE_SIZE, ubo.buffer),
                        VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, VK_SHADER_STAGE_MESH_BIT_EXT)
                .build();

        descriptorSetLayout = results.layout();
        descriptorSet = results.descriptorSet();
    }

    @Override
    public void initBuffers() {
        ubo = createBufferVMA(
                vmaAllocator,
                Float.BYTES * 3 * 16,
                VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                VMA_MEMORY_USAGE_AUTO,
                VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT |
                        VMA_ALLOCATION_CREATE_HOST_ACCESS_ALLOW_TRANSFER_INSTEAD_BIT);
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

    public void updateUBO() {
        var alignment = Float.BYTES * 3 * 16;
        var bufferSize = alignment;

        var bw = new BufferWriter(vmaAllocator, ubo, alignment, bufferSize);
        bw.mapBuffer();
        
        controller.mainCamera.getPerspectiveProjectionMatrix().setValuesToBuffer(bw.buffer);
        Matrix.getIdentityMatrix(4).setValuesToBuffer(bw.buffer);
        controller.mainCamera.worldToObject.setValuesToBuffer(bw.buffer);

        bw.unmapBuffer();
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
