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
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static Kurama.Vulkan.ShaderSPIRVUtils.ShaderKind.FRAGMENT_SHADER;
import static Kurama.Vulkan.ShaderSPIRVUtils.ShaderKind.VERTEX_SHADER;
import static Kurama.Vulkan.ShaderSPIRVUtils.compileShaderFile;
import static Kurama.Vulkan.VulkanUtilities.*;
import static Kurama.utils.Logger.log;
import static java.util.stream.Collectors.toSet;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.KHRMultiview.VK_KHR_MULTIVIEW_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_1_FEATURES;

public class AnaglyphRenderer extends RenderingEngine {
    public Set<String> DEVICE_EXTENSIONS =
            Stream.of(VK_KHR_SWAPCHAIN_EXTENSION_NAME,
                            VK_KHR_MULTIVIEW_EXTENSION_NAME)
                    .collect(toSet());
    public int MAXOBJECTS = 10000;
    public static final int MAX_FRAMES_IN_FLIGHT = 2;
    public long surface;
    public int numOfSamples = VK_SAMPLE_COUNT_1_BIT;
    public long minUniformBufferOffsetAlignment = 64;
    public VkPhysicalDeviceProperties gpuProperties;
    public AllocatedBuffer gpuSceneBuffer;
    public int graphicsQueueFamilyIndex;
    public VkQueue graphicsQueue;
    public VkQueue presentQueue;
    public long swapChain;

    public static class RenderPass {

        public List<ViewRenderPassFrame> frames;
        public Map<Integer, ViewRenderPassFrame> imagesToFrameMap;
        public int currentFrameIndex = 0;
        public List<Long> frameBuffers;
        public List<SwapChainAttachment> swapChainAttachments;
        public long renderPass;
    }

    public int swapChainImageFormat;
    public VkExtent2D swapChainExtent;

    public static class MultiViewRenderPass {

        public List<Long> frameBuffers;
        public List<AnaglyphMultiViewRenderPassFrame> frames;
        public Map<Integer, AnaglyphMultiViewRenderPassFrame> imagesToFrameMap;
        public int currentFrameIndex = 0;
        public FrameBufferAttachment depthAttachment;
        public FrameBufferAttachment colorAttachment;

        public long renderPass;

        // Global Descriptor set contains the camera data and other scene parameters
        public long globalDescriptorSetLayout;

        // This contains the object transformation matrices
        public long objectDescriptorSetLayout;

        // TODO: Refactor this into separate Material type class
        public long singleTextureSetLayout;
        public long singleTextureDescriptorSet;
        public long textureSampler;
    }
    public RenderPass viewRenderPass = new RenderPass();
    public MultiViewRenderPass multiViewRenderPass = new MultiViewRenderPass();
    public long descriptorPool;

    public long viewPipelineLayout;
    public long viewGraphicsPipeline;
    public long multiViewPipelineLayout;
    public long multiViewGraphicsPipeline;

    // The global Command pool and buffer are currently used for tasks such as image loading and transformations
    public long globalCommandPool;
    public VkCommandBuffer globalCommandBuffer;
    public boolean framebufferResize;
    public GPUCameraData gpuCameraDataLeft;
    public GPUCameraData gpuCameraDataRight;
    public GPUSceneData gpuSceneData;
    public SingleTimeCommandContext singleTimeCommandContext;
    public HashMap<String, TextureVK> loadedTextures;
    public int multiViewNumLayers = 2;
    public boolean msaaEnabled = false;

    public int tempSwap = 0;
    public long vmaAllocator;
    public DisplayVulkan display;
    public AnaglyphGame game;

    public AnaglyphRenderer(Game game) {
            super(game);
            this.game = (AnaglyphGame) game;
        }

        @Override
        public void init(Scene scene) {
            this.display = game.display;
            loadedTextures = new HashMap<>();
            initVulkan();
        }

        public void render(List<Renderable> renderables) {
            drawFrame(renderables);
        }

        public void initVulkan() {
            VulkanUtilities.createInstance("Vulkan game", "Kurama Engine");
        VulkanUtilities.setupDebugMessenger();

        surface = createSurface(instance, display.window);
        physicalDevice = pickPhysicalDevice(instance, surface, DEVICE_EXTENSIONS);
        createLogicalDevice();

        gpuProperties = getGPUProperties(physicalDevice);
        numOfSamples = getMaxUsableSampleCount(gpuProperties);
        numOfSamples = 1;

        minUniformBufferOffsetAlignment = getMinBufferOffsetAlignment(gpuProperties);

        vmaAllocator = createAllocator(physicalDevice, device, instance);

        initializeFrames();
        initSyncObjects();
        createFrameCommandPoolsAndBuffers();

        createGlobalCommandPool();
        createGlobalCommandBuffer();

        createSwapChain();
        createSwapChainImageViews();

        createColorAttachment();
        createMultiViewDepthAttachment();

        createRenderPass();

        createViewFramebuffers();

        // Descriptor set layout is needed when both defining the pipelines, and when creating the descriptor sets
        initDescriptors();
        deletionQueue.add(() -> vmaDestroyBuffer(vmaAllocator, gpuSceneBuffer.buffer, gpuSceneBuffer.allocation));
        deletionQueue.add(() -> viewRenderPass.frames.forEach(ViewRenderPassFrame::cleanUp));

        createGraphicsPipeline();
        deletionQueue.add(() -> cleanupSwapChain());
    }

    public void recordViewCommandBuffer(List<Renderable> renderables, ViewRenderPassFrame currentFrame, long frameBuffer, int frameIndex) {
    }

        public void recordMultiViewCommandBuffer(List<Renderable> renderables, AnaglyphMultiViewRenderPassFrame currentFrame, long frameBuffer, int frameIndex) {

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

                vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, viewGraphicsPipeline);

                var uniformOffset = (int) (padUniformBufferSize(GPUSceneData.SIZEOF, minUniformBufferOffsetAlignment) * frameIndex);
                var pUniformOffset = stack.mallocInt(1);
                pUniformOffset.put(0, uniformOffset);

                vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                        viewPipelineLayout, 0, stack.longs(currentFrame.globalDescriptorSet), pUniformOffset);

                vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                        viewPipelineLayout, 1, stack.longs(currentFrame.objectDescriptorSet), null);

                vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                        viewPipelineLayout, 2, stack.longs(multiViewRenderPass.singleTextureDescriptorSet), null);

                Mesh previousMesh = null;

                for(int i = 0; i < renderables.size(); i++) {
                    var renderable = renderables.get(i);
                    var model = renderable.model;

                    if(previousMesh != renderable.mesh) {
                        LongBuffer offsets = stack.longs(0);
                        LongBuffer vertexBuffers = stack.longs(renderable.vertexBuffer.buffer);
                        vkCmdBindVertexBuffers(commandBuffer, 0, vertexBuffers, offsets);
                        vkCmdBindIndexBuffer(commandBuffer, renderable.indexBuffer.buffer, 0, VK_INDEX_TYPE_UINT32);
                    }
                    previousMesh = renderable.mesh;

                    MeshPushConstants pushConstant = new MeshPushConstants();
                    pushConstant.renderMatrix = model.objectToWorldMatrix;

                    if(i == 0) {
                        pushConstant.data = new Vector(0f,0f,1f,1f);
                    }
                    else {
                        pushConstant.data = new Vector(1f,1f,0f,1f);
                    }

                    vkCmdPushConstants(commandBuffer,
                            viewPipelineLayout,
                            VK_SHADER_STAGE_VERTEX_BIT,
                            0,
                            pushConstant.getAsFloatBuffer());

                    vkCmdDrawIndexed(commandBuffer, renderable.mesh.indices.size(), 1, 0, 0, i);
                }
            }
            vkCmdEndRenderPass(commandBuffer);


            if (vkEndCommandBuffer(commandBuffer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to record command buffer");
            }
        }

    }

    public void loadTextures(List<Renderable> renderables) {
        for (var renderable : renderables) {
            var material = renderable.getMaterial();
            loadTexture((TextureVK) material.texture);
        }
    }

    public void loadTexture(TextureVK texture) {
        if (texture == null || texture.fileName == null || loadedTextures.containsKey(texture.fileName)) return;

        TextureVK.createTextureImage(graphicsQueue, vmaAllocator, singleTimeCommandContext, texture);
        TextureVK.createTextureImageView(texture);

        loadedTextures.put(texture.fileName, texture);
    }

    public void performBufferDataUpdates(List<Renderable> renderables, int currentFrameIndex) {
        updateCameraGPUDataInMemory(currentFrameIndex);
        updateSceneGPUDataInMemory(currentFrameIndex);
        updateObjectBufferDataInMemory(renderables, currentFrameIndex, multiViewRenderPass.frames.get(currentFrameIndex));
    }

    public void drawFrame(List<Renderable> renderables) {

        try(MemoryStack stack = stackPush()) {

            var thisFrame = viewRenderPass.frames.get(viewRenderPass.currentFrameIndex);

            vkWaitForFences(device, thisFrame.pFence(), true, UINT64_MAX);

            IntBuffer pImageIndex = stack.mallocInt(1);

            int vkResult = vkAcquireNextImageKHR(device, swapChain, UINT64_MAX,
                    thisFrame.imageAvailableSemaphore(), VK_NULL_HANDLE, pImageIndex);

            if(vkResult == VK_ERROR_OUT_OF_DATE_KHR) {
                recreateSwapChain();
                return;
            } else if(vkResult != VK_SUCCESS) {
                throw new RuntimeException("Cannot get image");
            }

            final int imageIndex = pImageIndex.get(0);

            performBufferDataUpdates(renderables, viewRenderPass.currentFrameIndex);

//            recordMultiViewCommandBuffer(renderables, thisFrame, viewRenderPass.frameBuffers.get(imageIndex), viewRenderPass.currentFrameIndex);
            recordViewCommandBuffer(renderables, thisFrame, viewRenderPass.frameBuffers.get(imageIndex), viewRenderPass.currentFrameIndex);


            if(viewRenderPass.imagesToFrameMap.containsKey(imageIndex)) {
                vkWaitForFences(device, viewRenderPass.frames.get(imageIndex).fence(), true, UINT64_MAX);
            }
            viewRenderPass.imagesToFrameMap.put(imageIndex, thisFrame);
            vkResetFences(device, thisFrame.pFence());

            // Submit rendering commands to GPU
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);

            submitInfo.waitSemaphoreCount(1);
            submitInfo.pWaitSemaphores(thisFrame.pImageAvailableSemaphore());
            submitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));

            submitInfo.pSignalSemaphores(thisFrame.pRenderFinishedSemaphore());
            submitInfo.pCommandBuffers(stack.pointers(thisFrame.commandBuffer));

            if((vkResult = vkQueueSubmit(graphicsQueue, submitInfo, thisFrame.fence())) != VK_SUCCESS) {
                vkResetFences(device, thisFrame.pFence());
                throw new RuntimeException("Failed to submit draw command buffer: " + vkResult);
            }

            // Display rendered image to screen
            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack);
            presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);
            presentInfo.pWaitSemaphores(thisFrame.pRenderFinishedSemaphore());
            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(stack.longs(swapChain));
            presentInfo.pImageIndices(pImageIndex);

            vkResult = vkQueuePresentKHR(presentQueue, presentInfo);

            if(vkResult == VK_ERROR_OUT_OF_DATE_KHR || vkResult == VK_SUBOPTIMAL_KHR || framebufferResize) {
                framebufferResize = false;
                recreateSwapChain();
            } else if(vkResult != VK_SUCCESS) {
                throw new RuntimeException("Failed to present swap chain image");
            }

            viewRenderPass.currentFrameIndex = (viewRenderPass.currentFrameIndex + 1) % MAX_FRAMES_IN_FLIGHT;
        }
    }

    public void uploadRenderable(Renderable renderable) {
        createIndexBufferForRenderable(renderable);
        createVertexBufferForRenderable(renderable);
    }

    public void createIndexBufferForRenderable(Renderable renderable) {
        try (var stack = stackPush()) {

            var bufferSize = Short.SIZE * renderable.mesh.indices.size();
            var stagingBuffer = createBufferVMA(vmaAllocator,
                    bufferSize,
                    VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VMA_MEMORY_USAGE_AUTO,
                    VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT);

            var data = stack.mallocPointer(1);

            vmaMapMemory(vmaAllocator, stagingBuffer.allocation, data);
            {
                memcpyInt(data.getByteBuffer(0, (int) bufferSize), renderable.mesh.indices);
            }
            vmaUnmapMemory(vmaAllocator, stagingBuffer.allocation);

            renderable.indexBuffer = createBufferVMA(vmaAllocator, bufferSize,
                    VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
                    VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE, VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT);

            Consumer<VkCommandBuffer> copyCmd = cmd -> {
                var copy = VkBufferCopy.calloc(1, stack);
                copy.dstOffset(0);
                copy.srcOffset(0);
                copy.size(bufferSize);
                vkCmdCopyBuffer(cmd, stagingBuffer.buffer, renderable.indexBuffer.buffer, copy);
            };

            submitImmediateCommand(copyCmd, singleTimeCommandContext, graphicsQueue);

            vmaDestroyBuffer(vmaAllocator, stagingBuffer.buffer, stagingBuffer.allocation);
        }
    }

    public void createVertexBufferForRenderable(Renderable renderable) {
        try (var stack = stackPush()) {

            var bufferSize = Vertex.SIZEOF * renderable.mesh.getVertices().size();
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
                    VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                    VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE, VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT);


            Consumer<VkCommandBuffer> copyCmd = cmd -> {
                var copy = VkBufferCopy.calloc(1, stack);
                copy.dstOffset(0);
                copy.srcOffset(0);
                copy.size(bufferSize);
                vkCmdCopyBuffer(cmd, stagingBuffer.buffer, renderable.vertexBuffer.buffer, copy);
            };

            submitImmediateCommand(copyCmd, singleTimeCommandContext, graphicsQueue);

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

    public void initDescriptors() {

        try (var stack = stackPush()) {

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

            // Allocate a descriptor pool that can create a max of 10 sets and 10 uniform buffers
            createDescriptorPool();

            // Creates a descriptor set layout with 2 bindings
            // binding 0 = GPU Camera Data
            // binding 1 = scene data

            // Creates a descriptor set layout with 1 binding
            // binding 0 = object matrices buffer

            // TODO: Refactor this with material pipeline abstraction
            // Creates a descriptor set layout with 1 binding
            // binding 0 = TextureSampler
            createMultiViewDescriptorSetLayouts();

            for(int i = 0;i < multiViewRenderPass.frames.size(); i++) {

                // uniform buffer for GPU camera data
                // A camera buffer is created for each frame
                multiViewRenderPass.frames.get(i).cameraBuffer
                        = createBufferVMA(
                        vmaAllocator,
                        cameraBufferSize,
                        VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                        VMA_MEMORY_USAGE_AUTO,
                        VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT |
                                VMA_ALLOCATION_CREATE_HOST_ACCESS_ALLOW_TRANSFER_INSTEAD_BIT);

                createGlobalDescriptorSetForFrame(multiViewRenderPass.frames.get(i), stack);

                multiViewRenderPass.frames.get(i).objectBuffer =
                        createBufferVMA(
                                vmaAllocator,
                                objectBufferSize,
                                VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
                                VMA_MEMORY_USAGE_AUTO,
                                VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT |
                                        VMA_ALLOCATION_CREATE_HOST_ACCESS_ALLOW_TRANSFER_INSTEAD_BIT);

                createObjectDescriptorSetForFrame(multiViewRenderPass.frames.get(i), stack);
            }

        }
    }

    // TODO: This should be specific to each texture/material
    public void createTextureDescriptorSet(long textureSampler, long imageView, MemoryStack stack) {
        var layout = stack.mallocLong(1);
        layout.put(0, multiViewRenderPass.singleTextureSetLayout);

        // Allocate a descriptor set
        VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack);
        allocInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
        allocInfo.descriptorPool(descriptorPool);
        allocInfo.pSetLayouts(layout);

        var pDescriptorSet = stack.mallocLong(1);
        if(vkAllocateDescriptorSets(device, allocInfo, pDescriptorSet) != VK_SUCCESS) {
            throw new RuntimeException("Failed to allocate descriptor sets");
        }
        multiViewRenderPass.singleTextureDescriptorSet = pDescriptorSet.get(0);

        //information about the buffer we want to point at in the descriptor
        var imageBufferInfo = VkDescriptorImageInfo.calloc(1, stack);
        imageBufferInfo.sampler(textureSampler);
        imageBufferInfo.imageView(imageView);
        imageBufferInfo.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

        VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(1, stack);
        var textureWrite =
                createWriteDescriptorSet(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                        multiViewRenderPass.singleTextureDescriptorSet,
                        imageBufferInfo,
                        0, stack);
        descriptorWrites.put(0, textureWrite);

        vkUpdateDescriptorSets(device, descriptorWrites, null);
    }

    public void createObjectDescriptorSetForFrame(AnaglyphMultiViewRenderPassFrame frame, MemoryStack stack) {
        var layout = stack.mallocLong(1);
        layout.put(0, multiViewRenderPass.objectDescriptorSetLayout);

        // Allocate a descriptor set
        VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack);
        allocInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
        allocInfo.descriptorPool(descriptorPool);
        allocInfo.pSetLayouts(layout);

        var pDescriptorSet = stack.mallocLong(1);
        if(vkAllocateDescriptorSets(device, allocInfo, pDescriptorSet) != VK_SUCCESS) {
            throw new RuntimeException("Failed to allocate descriptor sets");
        }
        frame.objectDescriptorSet = pDescriptorSet.get(0);

        //information about the buffer we want to point at in the descriptor
        var objectBufferSize = (int)(padUniformBufferSize(GPUObjectData.SIZEOF, minUniformBufferOffsetAlignment)) * MAXOBJECTS;

        VkDescriptorBufferInfo.Buffer objectBufferInfo = VkDescriptorBufferInfo.calloc(1, stack);
        objectBufferInfo.offset(0);
        objectBufferInfo.range(objectBufferSize);
        objectBufferInfo.buffer(frame.objectBuffer.buffer);

        VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(1, stack);
        var objectWrite =
                createWriteDescriptorSet(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER,
                        frame.objectDescriptorSet,
                        objectBufferInfo,
                        0, stack);
        descriptorWrites.put(0, objectWrite);

        vkUpdateDescriptorSets(device, descriptorWrites, null);
    }

    public void createGlobalDescriptorSetForFrame(AnaglyphMultiViewRenderPassFrame frame, MemoryStack stack) {

        var layout = stack.mallocLong(1);
        layout.put(0, multiViewRenderPass.globalDescriptorSetLayout);

        // Allocate a descriptor set
        VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack);
        allocInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
        allocInfo.descriptorPool(descriptorPool);
        allocInfo.pSetLayouts(layout);

        var pDescriptorSet = stack.mallocLong(1);

        if(vkAllocateDescriptorSets(device, allocInfo, pDescriptorSet) != VK_SUCCESS) {
            throw new RuntimeException("Failed to allocate descriptor sets");
        }
        frame.globalDescriptorSet = pDescriptorSet.get(0);

        //information about the buffer we want to point at in the descriptor
        var cameraBufferSize = (int)(padUniformBufferSize(GPUCameraData.SIZEOF, minUniformBufferOffsetAlignment)) * multiViewNumLayers;

        VkDescriptorBufferInfo.Buffer cameraBufferInfo = VkDescriptorBufferInfo.calloc(1, stack);
        cameraBufferInfo.offset(0);
        cameraBufferInfo.range(cameraBufferSize);
        cameraBufferInfo.buffer(frame.cameraBuffer.buffer);

        VkDescriptorBufferInfo.Buffer sceneBufferInfo = VkDescriptorBufferInfo.calloc(1, stack);
        sceneBufferInfo.offset(0);
        sceneBufferInfo.range(GPUSceneData.SIZEOF);
        sceneBufferInfo.buffer(gpuSceneBuffer.buffer);

        VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(2, stack);
        var gpuCameraWrite =
                createWriteDescriptorSet(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER,
                        frame.globalDescriptorSet,
                        cameraBufferInfo,
                        0, stack);

        var sceneWrite =
                createWriteDescriptorSet(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC,
                        frame.globalDescriptorSet,
                        sceneBufferInfo, 1, stack);

        descriptorWrites.put(0, gpuCameraWrite);
        descriptorWrites.put(1, sceneWrite);

        vkUpdateDescriptorSets(device, descriptorWrites, null);
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
        createRenderPass();
        createGraphicsPipeline();
        createColorAttachment();
        createMultiViewDepthAttachment();
        createViewFramebuffers();

        createGlobalCommandBuffer();
        recreateFrameCommandBuffers();
    }

    private void cleanupSwapChain() {

        viewRenderPass.frameBuffers.forEach(fb -> vkDestroyFramebuffer(device, fb, null));

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
                    1,
                    numOfSamples,
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
                            1,
                            stack
                    );

            depthAttachment.imageView = createImageView(viewInfo, device);

            // Explicitly transitioning the depth image
            submitImmediateCommand((cmd) -> {
                        transitionImageLayout(
                                depthAttachment.allocatedImage.image, depthFormat,
                                VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
                                1,1, cmd);
                    },
                    singleTimeCommandContext, graphicsQueue);

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

            QueueFamilyIndices indices = findQueueFamilies(physicalDevice, surface);

            if(!indices.graphicsFamily.equals(indices.presentFamily)) {
                createInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT);
                createInfo.pQueueFamilyIndices(stack.ints(indices.graphicsFamily, indices.presentFamily));
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
                                stack
                        );
                swapChainImageAttachment.swapChainImageView = createImageView(viewInfo, device);
            }

        }
    }

    public void createMultiViewRenderPass() {

    }

    public void createMultiViewPipeline() {

    }

    public void createMultiViewDescriptors() {

    }

    public void createRenderPass() {

        try(MemoryStack stack = stackPush()) {

            VkAttachmentDescription.Buffer attachments;
            VkAttachmentReference.Buffer attachmentRefs;

            attachments = VkAttachmentDescription.calloc(2, stack);
            attachmentRefs = VkAttachmentReference.calloc(2, stack);

            // MSA image
            var colorAttachment = attachments .get(0);
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

            VkSubpassDependency.Buffer dependency = VkSubpassDependency.calloc(1, stack);
            dependency.srcSubpass(VK_SUBPASS_EXTERNAL);
            dependency.dstSubpass(0);
            dependency.srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
            dependency.srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
            dependency.dstStageMask(VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT);
            dependency.dstAccessMask(VK_ACCESS_MEMORY_READ_BIT);
            dependency.dependencyFlags(VK_DEPENDENCY_BY_REGION_BIT);

            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack);
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO);
            renderPassInfo.pAttachments(attachments);
            renderPassInfo.pSubpasses(subpass);
            renderPassInfo.pDependencies(dependency);

//            var viewMask = stack.ints(Integer.parseInt("00000011", 2));
//            var correlationMask = stack.ints(Integer.parseInt("00000011", 2));
//
//            var multiviewCreateInfo = VkRenderPassMultiviewCreateInfo.calloc();
//            multiviewCreateInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_MULTIVIEW_CREATE_INFO);
//            multiviewCreateInfo.pViewMasks(viewMask);
//            multiviewCreateInfo.pCorrelationMasks(correlationMask);
//
//            renderPassInfo.pNext(multiviewCreateInfo);

            LongBuffer pRenderPass = stack.mallocLong(1);

            if(vkCreateRenderPass(device, renderPassInfo, null, pRenderPass) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create render pass");
            }

            viewRenderPass.renderPass = pRenderPass.get(0);
        }
    }

    public void createGraphicsPipeline() {

        try(MemoryStack stack = stackPush()) {

            // Let's compile the GLSL shaders into SPIR-V at runtime using the shaderc library
            // Check ShaderSPIRVUtils class to see how it can be done
            ShaderSPIRVUtils.SPIRV vertShaderSPIRV = compileShaderFile("shaders/shader.vert", VERTEX_SHADER);
            ShaderSPIRVUtils.SPIRV fragShaderSPIRV = compileShaderFile("shaders/shader.frag", FRAGMENT_SHADER);

            long vertShaderModule = createShaderModule(vertShaderSPIRV.bytecode(), device);
            long fragShaderModule = createShaderModule(fragShaderSPIRV.bytecode(), device);

            ByteBuffer entryPoint = stack.UTF8("main");

            VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.calloc(2, stack);

            VkPipelineShaderStageCreateInfo vertShaderStageInfo = shaderStages.get(0);

            vertShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            vertShaderStageInfo.stage(VK_SHADER_STAGE_VERTEX_BIT);
            vertShaderStageInfo.module(vertShaderModule);
            vertShaderStageInfo.pName(entryPoint);

            VkPipelineShaderStageCreateInfo fragShaderStageInfo = shaderStages.get(1);

            fragShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            fragShaderStageInfo.stage(VK_SHADER_STAGE_FRAGMENT_BIT);
            fragShaderStageInfo.module(fragShaderModule);
            fragShaderStageInfo.pName(entryPoint);

            // ===> VERTEX STAGE <===

            VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc(stack);
            vertexInputInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
            vertexInputInfo.pVertexBindingDescriptions(Vertex.getBindingDescription());
            vertexInputInfo.pVertexAttributeDescriptions(Vertex.getAttributeDescriptions());

            // ===> ASSEMBLY STAGE <===

            VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack);
            inputAssembly.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
            inputAssembly.topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
            inputAssembly.primitiveRestartEnable(false);

            // ===> VIEWPORT & SCISSOR

            VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
            viewport.x(0.0f);
            viewport.y(0.0f);
            viewport.width(swapChainExtent.width());
            viewport.height(swapChainExtent.height());
            viewport.minDepth(0.0f);
            viewport.maxDepth(1.0f);

            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
            scissor.offset(VkOffset2D.calloc(stack).set(0, 0));
            scissor.extent(swapChainExtent);

            VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack);
            viewportState.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);
            viewportState.pViewports(viewport);
            viewportState.pScissors(scissor);

            // ===> RASTERIZATION STAGE <===

            VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack);
            rasterizer.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
            rasterizer.depthClampEnable(false);
            rasterizer.rasterizerDiscardEnable(false);
//            rasterizer.polygonMode(VK_POLYGON_MODE_FILL);
            rasterizer.polygonMode(VK_POLYGON_MODE_FILL);
            rasterizer.lineWidth(1.0f);
            rasterizer.cullMode(VK_CULL_MODE_BACK_BIT);
            rasterizer.frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE);
            rasterizer.depthBiasEnable(false);

            VkPipelineDepthStencilStateCreateInfo depthStencil = VkPipelineDepthStencilStateCreateInfo.calloc(stack);
            depthStencil.sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO);
            depthStencil.depthTestEnable(true);
            depthStencil.depthWriteEnable(true);
            depthStencil.depthCompareOp(VK_COMPARE_OP_LESS_OR_EQUAL);
            depthStencil.depthBoundsTestEnable(false);
            depthStencil.minDepthBounds(0.0f); // Optional
            depthStencil.maxDepthBounds(1.0f); // Optional
            depthStencil.stencilTestEnable(false);
            depthStencil.front();
            depthStencil.back();

            // ===> MULTISAMPLING <===

            VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack);
            multisampling.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
            multisampling.sampleShadingEnable(false);

            if(msaaEnabled) {
                multisampling.sampleShadingEnable(true);
                multisampling.rasterizationSamples(numOfSamples);
                multisampling.minSampleShading(0.2f);
            }
            else {
                multisampling.rasterizationSamples(1);
            }

            // ===> COLOR BLENDING <===

            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(1, stack);
            colorBlendAttachment.colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT);
            colorBlendAttachment.blendEnable(false);

            VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack);
            colorBlending.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
            colorBlending.logicOpEnable(false);
            colorBlending.logicOp(VK_LOGIC_OP_COPY);
            colorBlending.pAttachments(colorBlendAttachment);
            colorBlending.blendConstants(stack.floats(0.0f, 0.0f, 0.0f, 0.0f));

            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack);
            pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
            pipelineLayoutInfo.pSetLayouts(stack.longs(multiViewRenderPass.globalDescriptorSetLayout, multiViewRenderPass.objectDescriptorSetLayout, multiViewRenderPass.singleTextureSetLayout));

            // Set push constants
            VkPushConstantRange.Buffer pushConstant = VkPushConstantRange.calloc(1, stack);
            pushConstant.offset(0);
            pushConstant.size(MeshPushConstants.SIZEOF);
            pushConstant.stageFlags(VK_SHADER_STAGE_VERTEX_BIT);

            pipelineLayoutInfo.pPushConstantRanges(pushConstant);

            // ===> PIPELINE LAYOUT CREATION <===

            LongBuffer pPipelineLayout = stack.longs(VK_NULL_HANDLE);

            if(vkCreatePipelineLayout(device, pipelineLayoutInfo, null, pPipelineLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create pipeline layout");
            }

            viewPipelineLayout = pPipelineLayout.get(0);

            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack);
            pipelineInfo.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
            pipelineInfo.pStages(shaderStages);
            pipelineInfo.pVertexInputState(vertexInputInfo);
            pipelineInfo.pInputAssemblyState(inputAssembly);
            pipelineInfo.pViewportState(viewportState);
            pipelineInfo.pRasterizationState(rasterizer);
            pipelineInfo.pMultisampleState(multisampling);
            pipelineInfo.pColorBlendState(colorBlending);
            pipelineInfo.layout(viewPipelineLayout);
            pipelineInfo.renderPass(viewRenderPass.renderPass);
            pipelineInfo.subpass(0);
            pipelineInfo.basePipelineHandle(VK_NULL_HANDLE);
            pipelineInfo.basePipelineIndex(-1);
            pipelineInfo.pDepthStencilState(depthStencil);

            LongBuffer pGraphicsPipeline = stack.mallocLong(1);

            if(vkCreateGraphicsPipelines(device, VK_NULL_HANDLE, pipelineInfo, null, pGraphicsPipeline) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create graphics pipeline");
            }

            viewGraphicsPipeline = pGraphicsPipeline.get(0);

            // ===> RELEASE RESOURCES <===

            vkDestroyShaderModule(device, vertShaderModule, null);
            vkDestroyShaderModule(device, fragShaderModule, null);

            vertShaderSPIRV.free();
            fragShaderSPIRV.free();
        }
    }

    public void createMultiViewFrameBuffer() {

    }

    public void createViewFramebuffers() {

        try(MemoryStack stack = stackPush()) {

            viewRenderPass.frameBuffers = new ArrayList<>();
            var attachments = stack.longs(VK_NULL_HANDLE);

            LongBuffer pFramebuffer = stack.mallocLong(1);

            // Lets allocate the create info struct once and just update the pAttachments field each iteration
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
        }
    }

    public void createGlobalCommandPool() {

        try(MemoryStack stack = stackPush()) {

            QueueFamilyIndices queueFamilyIndices = findQueueFamilies(physicalDevice, surface);

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

            graphicsQueueFamilyIndex = indices.graphicsFamily;
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
        }
    }

    public void createMultiViewDescriptorSetLayouts() {

        try (var stack = stackPush()) {

            var objectBinding = createDescriptorSetLayoutBinding(0, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_VERTEX_BIT, stack);

            var cameraBufferBinding = createDescriptorSetLayoutBinding(0, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, VK_SHADER_STAGE_VERTEX_BIT, stack);
            var sceneBinding = createDescriptorSetLayoutBinding(1, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC, VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT, stack);

            var textureBinding = createDescriptorSetLayoutBinding(0, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, VK_SHADER_STAGE_FRAGMENT_BIT, stack);

            multiViewRenderPass.objectDescriptorSetLayout = createDescriptorSetLayout(new VkDescriptorSetLayoutBinding[]{objectBinding}, stack);
            multiViewRenderPass.globalDescriptorSetLayout = createDescriptorSetLayout(new VkDescriptorSetLayoutBinding[]{cameraBufferBinding, sceneBinding}, stack);
            multiViewRenderPass.singleTextureSetLayout = createDescriptorSetLayout(new VkDescriptorSetLayoutBinding[]{textureBinding}, stack);

            deletionQueue.add(() -> vkDestroyDescriptorSetLayout(device, multiViewRenderPass.objectDescriptorSetLayout, null));
            deletionQueue.add(() -> vkDestroyDescriptorSetLayout(device, multiViewRenderPass.globalDescriptorSetLayout, null));
            deletionQueue.add(() -> vkDestroyDescriptorSetLayout(device, multiViewRenderPass.singleTextureSetLayout, null));
        }
    }

    public void createColorAttachment() {
        try(var stack = stackPush()) {

            var colorAttachment = new FrameBufferAttachment();

            var extent = VkExtent3D.calloc(stack).width(swapChainExtent.width()).height(swapChainExtent.height()).depth(1);

            var imageInfo = createImageCreateInfo(
                    swapChainImageFormat,
                    VK_IMAGE_USAGE_TRANSIENT_ATTACHMENT_BIT | VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT,
                    extent,
                    1,
                    VK_IMAGE_TILING_OPTIMAL,
                    1,
                    numOfSamples,
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
                            1,
                            stack
                    );

            colorAttachment.imageView = createImageView(viewInfo, device);

            submitImmediateCommand((cmd) -> {
                transitionImageLayout(
                        colorAttachment.allocatedImage.image, swapChainImageFormat,
                        VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                        1, 1, cmd);
            }, singleTimeCommandContext, graphicsQueue);

            multiViewRenderPass.colorAttachment = colorAttachment;
        }
    }

    // Allocate a descriptor pool that can create a max of 10 sets and 10 uniform buffers
    public void createDescriptorPool() {
        try (var stack = stackPush()) {

            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(4, stack);

            VkDescriptorPoolSize uniformBufferPoolSize  = poolSizes.get(0);
            uniformBufferPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
            uniformBufferPoolSize.descriptorCount(10);

            VkDescriptorPoolSize uniformBufferDynamicPoolSize  = poolSizes.get(1);
            uniformBufferDynamicPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC);
            uniformBufferDynamicPoolSize.descriptorCount(10);

            VkDescriptorPoolSize storageBufferPoolSize  = poolSizes.get(2);
            storageBufferPoolSize.type(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER);
            storageBufferPoolSize.descriptorCount(10);

            VkDescriptorPoolSize textureSamplerPoolSize  = poolSizes.get(3);
            textureSamplerPoolSize.type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
            textureSamplerPoolSize.descriptorCount(10);

            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
            poolInfo.pPoolSizes(poolSizes);
            poolInfo.maxSets(10);

            LongBuffer pDescriptorPool = stack.mallocLong(1);

            if(vkCreateDescriptorPool(device, poolInfo, null, pDescriptorPool) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor pool");
            }

            descriptorPool = pDescriptorPool.get(0);
            deletionQueue.add(() -> vkDestroyDescriptorPool(device, descriptorPool, null));
        }
    }

    public void updateCameraGPUDataInMemory(int currentFrame) {
        try(MemoryStack stack = stackPush()) {

            PointerBuffer data = stack.mallocPointer(1);
            vmaMapMemory(vmaAllocator, multiViewRenderPass.frames.get(currentFrame).cameraBuffer.allocation, data);
            {
                var offset = (int)(padUniformBufferSize(GPUCameraData.SIZEOF, minUniformBufferOffsetAlignment));
                int bufferSize = (int) (padUniformBufferSize(GPUCameraData.SIZEOF, minUniformBufferOffsetAlignment) * multiViewNumLayers);
                var buffer = data.getByteBuffer(bufferSize);

                GPUCameraData.memcpy(buffer, gpuCameraDataLeft);
//                if(tempSwap == 0) {
//                    GPUCameraData.memcpy(buffer, gpuCameraDataLeft);
//                    tempSwap = 1;
//                }
//                else {
//                    GPUCameraData.memcpy(buffer, gpuCameraDataRight);
//                    tempSwap = 0;
//                }

                buffer.position(offset);
                GPUCameraData.memcpy(buffer, gpuCameraDataRight);
            }
            vmaUnmapMemory(vmaAllocator, multiViewRenderPass.frames.get(currentFrame).cameraBuffer.allocation);
        }
    }

    public void updateSceneGPUDataInMemory(int currentFrame) {
        try(MemoryStack stack = stackPush()) {
            PointerBuffer data = stack.mallocPointer(1);
            vmaMapMemory(vmaAllocator, gpuSceneBuffer.allocation, data);
            {
                var offset = (int)(padUniformBufferSize(GPUSceneData.SIZEOF, minUniformBufferOffsetAlignment) * currentFrame);
                int bufferSize = (int) (padUniformBufferSize(GPUSceneData.SIZEOF, minUniformBufferOffsetAlignment) * MAX_FRAMES_IN_FLIGHT);

                var buffer = data.getByteBuffer(bufferSize);
                buffer.position(offset);
                GPUSceneData.memcpy(buffer, gpuSceneData);
            }
            vmaUnmapMemory(vmaAllocator, gpuSceneBuffer.allocation);
        }
    }

    public void updateObjectBufferDataInMemory(List<Renderable> renderables, int currentFrameIndex, AnaglyphMultiViewRenderPassFrame frame) {
        try (var stack = stackPush()) {
            PointerBuffer data = stack.mallocPointer(1);
            vmaMapMemory(vmaAllocator, frame.objectBuffer.allocation, data);

            var alignmentSize = (int)(padUniformBufferSize(RenderingEngineVulkan.GPUObjectData.SIZEOF, minUniformBufferOffsetAlignment));
            int bufferSize = alignmentSize * MAXOBJECTS;

            var buffer = data.getByteBuffer(bufferSize);
            int offset = 0;

            for(int i = 0; i < renderables.size(); i++) {
                buffer.position(offset);

                var renderable = renderables.get(i);
                var gpuObjectData = new GPUObjectData();
                gpuObjectData.modelMatrix = renderable.model.objectToWorldMatrix;
                GPUObjectData.memcpy(buffer, gpuObjectData);

                offset += alignmentSize;
            }

            vmaUnmapMemory(vmaAllocator, frame.objectBuffer.allocation);
        }
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

            var newFrame = new AnaglyphMultiViewRenderPassFrame();
            newFrame.vmaAllocator = vmaAllocator;
            multiViewRenderPass.frames.add(newFrame);
        }
    }

    public void createFrameCommandPoolsAndBuffers() {

        try (var stack = stackPush()) {

            QueueFamilyIndices queueFamilyIndices = findQueueFamilies(physicalDevice, surface);

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

            for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {

                if (vkCreateSemaphore(device, semaphoreInfo, null, pImageAvailableSemaphore) != VK_SUCCESS
                        || vkCreateSemaphore(device, semaphoreInfo, null, pRenderFinishedSemaphore) != VK_SUCCESS
                        || vkCreateFence(device, fenceInfo, null, pFence) != VK_SUCCESS) {

                    throw new RuntimeException("Failed to create synchronization objects for the frame " + i);
                }

                viewRenderPass.frames.get(i).imageAvailableSemaphore = pImageAvailableSemaphore.get(0);
                viewRenderPass.frames.get(i).renderFinishedSemaphore = pRenderFinishedSemaphore.get(0);
                viewRenderPass.frames.get(i).fence = pFence.get(0);
            }

            // Create fence and commandPool/buffer for immediate upload context
            singleTimeCommandContext = new SingleTimeCommandContext();

            singleTimeCommandContext.fence = createFence(VkFenceCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO));

            singleTimeCommandContext.commandPool =  createCommandPool(device,
                    createCommandPoolCreateInfo(graphicsQueueFamilyIndex, 0, stack), stack);

            var cmdAllocInfo = createCommandBufferAllocateInfo(singleTimeCommandContext.commandPool, 1, VK_COMMAND_BUFFER_LEVEL_PRIMARY, stack);
            singleTimeCommandContext.commandBuffer = createCommandBuffer(device, cmdAllocInfo, stack);

            deletionQueue.add(() -> singleTimeCommandContext.cleanUp(device));
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

    public class MeshPushConstants {
        public static int SIZEOF = (16 + 4) * Float.BYTES;
        public Vector data;
        public Matrix renderMatrix;

        public MeshPushConstants() {
            renderMatrix = Matrix.getIdentityMatrix(4);
            data = new Vector(4, 0);
        }
        public FloatBuffer getAsFloatBuffer() {
            FloatBuffer res = MemoryUtil.memAllocFloat((Float.BYTES * 4 * 4) + (Float.BYTES * 4));

            for(Vector c:renderMatrix.convertToColumnVectorArray()) {
                for(float val: c.getData()) {
                    res.put(val);
                }
            }

            for(float v: data.getData()) {
                res.put(v);
            }
            res.flip();
            return res;
        }

        public static void memcpy(FloatBuffer buffer, MeshPushConstants data) {
            data.renderMatrix.setValuesToBuffer(buffer);
            data.data.setValuesToBuffer(buffer);
        }
    }

    public static class GPUCameraData {

        public static final int SIZEOF = 3 * 16 * Float.BYTES;
        public Matrix projview;
        public Matrix view;
        public Matrix proj;

        public GPUCameraData() {
            projview = Matrix.getIdentityMatrix(4);
            view = Matrix.getIdentityMatrix(4);
            proj = Matrix.getIdentityMatrix(4);
        }

        public static void memcpy(ByteBuffer buffer, GPUCameraData data) {
            data.projview.setValuesToBuffer(buffer);
            data.view.setValuesToBuffer(buffer);
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

    public static class Vertex {
        public static final int SIZEOF = (3 + 2 + 3) * Float.BYTES;
        public static final int OFFSETOF_POS = 0;
        public static final int OFFSETOF_TEXCOORDS = 3 * Float.BYTES;
        public static final int OFFSETOF_NORMAL = 5 * Float.BYTES;

        public static VkVertexInputBindingDescription.Buffer getBindingDescription() {

            VkVertexInputBindingDescription.Buffer bindingDescription =
                    VkVertexInputBindingDescription.calloc(1);

            bindingDescription.binding(0);
            bindingDescription.stride(Vertex.SIZEOF);
            bindingDescription.inputRate(VK_VERTEX_INPUT_RATE_VERTEX);

            return bindingDescription;
        }

        public static VkVertexInputAttributeDescription.Buffer getAttributeDescriptions() {

            var attributeDescriptions = VkVertexInputAttributeDescription.calloc(3);

            // Position
            VkVertexInputAttributeDescription posDescription = attributeDescriptions.get(0);
            posDescription.binding(0);
            posDescription.location(0);
            posDescription.format(VK_FORMAT_R32G32B32_SFLOAT);
            posDescription.offset(OFFSETOF_POS);

            // textCoords
            var texDescription = attributeDescriptions.get(1);
            texDescription.binding(0);
            texDescription.location(1);
            texDescription.format(VK_FORMAT_R32G32_SFLOAT);
            texDescription.offset(OFFSETOF_TEXCOORDS);

            // Normals
            VkVertexInputAttributeDescription normalDescription = attributeDescriptions.get(2);
            normalDescription.binding(0);
            normalDescription.location(2);
            normalDescription.format(VK_FORMAT_R32G32B32_SFLOAT);
            normalDescription.offset(OFFSETOF_NORMAL);

            return attributeDescriptions.rewind();
        }

    }

}
