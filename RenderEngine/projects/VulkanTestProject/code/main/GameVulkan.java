package main;

import Kurama.ComponentSystem.components.model.Model;
import Kurama.Math.Matrix;
import Kurama.Math.Quaternion;
import Kurama.Math.Vector;
import Kurama.Mesh.Mesh;
import Kurama.Mesh.Texture;
import Kurama.Vulkan.Frame;
import Kurama.Vulkan.ShaderSPIRVUtils;
import Kurama.Vulkan.Vulkan;
import Kurama.camera.Camera;
import Kurama.display.DisplayVulkan;
import Kurama.game.Game;
import Kurama.geometry.assimp.AssimpStaticLoader;
import Kurama.inputs.InputLWJGL;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static Kurama.Vulkan.ShaderSPIRVUtils.ShaderKind.FRAGMENT_SHADER;
import static Kurama.Vulkan.ShaderSPIRVUtils.ShaderKind.VERTEX_SHADER;
import static Kurama.Vulkan.ShaderSPIRVUtils.compileShaderFile;
import static Kurama.Vulkan.Vulkan.*;
import static Kurama.utils.Logger.log;
import static Kurama.utils.Logger.logError;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT;
import static org.lwjgl.vulkan.KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class GameVulkan extends Game {

    // Below are Vulkan specific variables
    public static final int MAX_FRAMES_IN_FLIGHT = 2;
    public long surface;
    public int msaaSamples = VK_SAMPLE_COUNT_1_BIT;
    public long minUniformBufferOffsetAlignment = 64;
    public VkPhysicalDeviceProperties gpuProperties;
    public long vertexBuffer;
    public long vertexBufferMemory;
    public long indexBuffer;
    public long indexBufferMemory;
    public VkQueue graphicsQueue;
    public VkQueue presentQueue;
    public long swapChain;
    public List<Long> swapChainImages;
    public int swapChainImageFormat;
    public VkExtent2D swapChainExtent;
    public List<Long> swapChainImageViews;
    public long depthImage;
    public long depthImageMemory;
    public long depthImageView;
    public long colorImage;
    public long colorImageMemory;
    public long colorImageView;
    public List<Long> swapChainFramebuffers;
    public long renderPass;
    public long descriptorPool;
    public long globalDescriptorSetLayout;
    private long pipelineLayout;
    private long graphicsPipeline;

    // The global Command pool and buffer are currently used for tasks such as image loading and transformations
    public long globalCommandPool;
    public VkCommandBuffer globalCommandBuffer;

    public List<Frame> inFlightFrames;
    public Map<Integer, Frame> imagesInFlight;
    public int currentFrame;
    public boolean framebufferResize;

    public DisplayVulkan display;
    public GPUCameraData gpuCameraData;
    public GPUSceneData gpuSceneData;
    public long gpuSceneDataBuffer;
    public long gpUSceneDataBufferMemory;
    public long vmaAllocator;

    // NON-VULKAN variables
    public Camera playerCamera;
    public float mouseXSensitivity = 20f;
    public float mouseYSensitivity = 20f;
    public float speed = 15f;
    public float speedMultiplier = 1;
    public float speedIncreaseMultiplier = 2;
    public boolean isGameRunning = true;

    public List<Model> models = new ArrayList<>();
    public float colorChangeAngle = 0;

    public GameVulkan(String threadName) {
        super(threadName);
        GRAPHICS_API = GraphicsApi.VULKAN;
    }

    @Override
    public void init() {

        renderingEngine = new RenderingEngineVulkan(this);
        display = new DisplayVulkan(this);
        display.resizeEvents.add(() -> this.framebufferResize = true);

        initVulkan();
        this.setTargetFPS(Integer.MAX_VALUE);
        this.shouldDisplayFPS = true;

        this.input = new InputLWJGL(this, display);

        playerCamera = new Camera(this,null, null, new Vector(new float[] {0,0,0}),45, 0.001f, 1000.0f,
                swapChainExtent.width(), swapChainExtent.height(), false, "playerCam");

        playerCamera.shouldUpdateValues = true;

        display.resizeEvents.add(() -> {
            playerCamera.renderResolution = display.windowResolution;
            playerCamera.setShouldUpdateValues(true);
        });
        this.gpuCameraData = new GPUCameraData();
        this.gpuCameraData.proj = playerCamera.getPerspectiveProjectionMatrix();
        gpuCameraData.proj.getData()[1][1] *= -1;

        this.gpuSceneData = new GPUSceneData();
        this.gpuSceneData.sunLightColor = new Vector(new float[]{1,1,1,1});
        this.gpuSceneData.ambientColor = new Vector(new float[]{1,1,1,1});
        this.gpuSceneData.fogDistance = new Vector(new float[]{200,200,0,0});
        this.gpuSceneData.sunlightDirection = new Vector(new float[]{0,-1,0,1});
        this.gpuSceneData.fogColor = new Vector(new float[]{1,1,1,1});

        display.disableCursor();
    }

    @Override
    public void tick() {
        glfwPollEvents();
        input.poll();

        cameraUpdates(this.timeDelta);

        if(isGameRunning) {

            // Camera updates
            playerCamera.velocity = playerCamera.velocity.add(playerCamera.acceleration.scalarMul(timeDelta));
            var detlaV = playerCamera.velocity.scalarMul(timeDelta);
            playerCamera.setPos(playerCamera.getPos().add(detlaV));

            if (playerCamera.shouldUpdateValues) {
                playerCamera.updateValues();
                playerCamera.setShouldUpdateValues(false);
                this.gpuCameraData.proj = playerCamera.getPerspectiveProjectionMatrix();
                gpuCameraData.proj.getData()[1][1] *= -1;

                playerCamera.setupTransformationMatrices();
                this.gpuCameraData.view = playerCamera.getWorldToObject();
            }

            playerCamera.setupTransformationMatrices();
            this.gpuCameraData.view = playerCamera.getWorldToObject();
            this.gpuCameraData.projview = this.gpuCameraData.proj.matMul(this.gpuCameraData.view);

            colorChangeAngle += 0.1 * timeDelta;
            this.gpuSceneData.ambientColor = new Vector(new float[]{(float) Math.sin(colorChangeAngle), 0, (float) Math.cos(colorChangeAngle), 1});

            // Call tick on all models
            models.forEach(m -> m.tick(null, input, timeDelta, false));
        }

        if(glfwWindowShouldClose(display.window)) {
            programRunning = false;
            isGameRunning = false;
        }
        input.reset();
    }

    public void cameraUpdates(float timeDelta) {
        Vector velocity = new Vector(3,0);

        if(input.keyDown(input.W)) {
            float cameraSpeed = this.speed * this.speedMultiplier;
            Vector[] rotationMatrix = this.playerCamera.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector x = rotationMatrix[0];
            Vector y = new Vector(new float[] {0,1,0});
            Vector z = x.cross(y);
            velocity = velocity.add((z.scalarMul(-cameraSpeed)));
        }

        if(input.keyDownOnce(input.ESCAPE)) {
            if(isGameRunning) {
                isGameRunning = false;
                display.enableCursor();
            }
            else {
                isGameRunning = true;
                display.disableCursor();
            }
        }

        if(input.keyDown(input.S)) {
            float cameraSpeed = this.speed * this.speedMultiplier;
            Vector[] rotationMatrix = this.playerCamera.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector x = rotationMatrix[0];
            Vector y = new Vector(new float[] {0,1,0});
            Vector z = x.cross(y);
            velocity = velocity.add(z.scalarMul(cameraSpeed));
        }

        if(input.keyDown(input.A)) {
            float cameraSpeed = this.speed * this.speedMultiplier;
            Vector[] rotationMatrix = this.playerCamera.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector v = rotationMatrix[0];
            velocity = velocity.add(v.scalarMul(-cameraSpeed));
        }

        if(input.keyDown(input.D)) {
            float cameraSpeed = this.speed * this.speedMultiplier;
            Vector[] rotationMatrix = this.playerCamera.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector v = rotationMatrix[0];
            velocity = velocity.add(v.scalarMul(cameraSpeed));
        }

        if(input.keyDown(input.SPACE)) {
            float cameraSpeed = this.speed * this.speedMultiplier;

            Vector v = new Vector(new float[] {0,1,0});
            velocity = velocity.add(v.scalarMul(cameraSpeed));
        }

        if(input.keyDown(input.LEFT_SHIFT)) {
            float cameraSpeed = this.speed * this.speedMultiplier;

            Vector v = new Vector(new float[] {0,1,0});
            velocity = velocity.add(v.scalarMul(-cameraSpeed));
        }

        if(input.keyDownOnce(input.LEFT_CONTROL)) {
            if(this.speedMultiplier == 1) this.speedMultiplier = this.speedIncreaseMultiplier;
            else this.speedMultiplier = 1;
        }

        if(input.keyDown(input.LEFT_ARROW)) {
            models.get(0).orientation = models.get(0).orientation.multiply(Quaternion.getQuaternionFromEuler(0, 0, 90 * timeDelta));
        }

        if(input.keyDown(input.RIGHT_ARROW)) {
            models.get(0).orientation = models.get(0).orientation.multiply(Quaternion.getQuaternionFromEuler(0, 0, -90 * timeDelta));
        }

        this.playerCamera.velocity = velocity;
        calculate3DCamMovement();
    }

    private void calculate3DCamMovement() {
        if (this.input.getDelta().getNorm() != 0 && this.isGameRunning) {

            float yawIncrease   = this.mouseXSensitivity * this.timeDelta * -this.input.getDelta().get(0);
            float pitchIncrease = this.mouseYSensitivity * this.timeDelta * -this.input.getDelta().get(1);

            Vector currentAngle = this.playerCamera.getOrientation().getPitchYawRoll();
            float currentPitch = currentAngle.get(0) + pitchIncrease;

            if(currentPitch >= 0 && currentPitch > 60) {
                pitchIncrease = 0;
            }
            else if(currentPitch < 0 && currentPitch < -60) {
                pitchIncrease = 0;
            }

            Quaternion pitch = Quaternion.getAxisAsQuat(new Vector(new float[] {1,0,0}),pitchIncrease);
            Quaternion yaw = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}),yawIncrease);

            Quaternion q = this.playerCamera.getOrientation();

            q = q.multiply(pitch);
            q = yaw.multiply(q);
            this.playerCamera.setOrientation(q);
        }
    }

    @Override
    public void render() {
        drawFrame();
    }

    public void recordCommandBuffer( Frame currentFrame, int frameIndex, long swapChainFrameBuffer) {

        var commandBuffer = currentFrame.commandBuffer;
        var descriptorSet = currentFrame.globalDescriptorSet;

        try (var stack = stackPush()) {

            if(vkResetCommandBuffer(commandBuffer, 0) != VK_SUCCESS) {
                throw new RuntimeException("Failed to begin recording command buffer");
            }

            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

            VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc(stack);
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO);

            renderPassInfo.renderPass(renderPass);

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

            renderPassInfo.framebuffer(swapChainFrameBuffer);

            vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);
            {
                vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline);

                LongBuffer vertexBuffers = stack.longs(vertexBuffer);
                LongBuffer offsets = stack.longs(0);
                vkCmdBindVertexBuffers(commandBuffer, 0, vertexBuffers, offsets);
                vkCmdBindIndexBuffer(commandBuffer, indexBuffer, 0, VK_INDEX_TYPE_UINT32);

                var uniformOffset = (int) (padUniformBufferSize(GPUSceneData.SIZEOF, minUniformBufferOffsetAlignment) * frameIndex);
                var pUniformOffset = stack.mallocInt(1);
                pUniformOffset.put(0, uniformOffset);

                vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                        pipelineLayout, 0, stack.longs(descriptorSet), pUniformOffset);

                int firstIndex = 0;
                int vertexOffset = 0;

                for(var model: models) {
                    MeshPushConstants pushConstant = new MeshPushConstants();
                    pushConstant.renderMatrix = model.objectToWorldMatrix;
                    pushConstant.data = new Vector(1,1,1,1);

                    vkCmdPushConstants(commandBuffer,
                            pipelineLayout,
                            VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
                            0,
                            pushConstant.getAsFloatBuffer());

                    vkCmdDrawIndexed(commandBuffer, model.meshes.get(0).indices.size(), 1, firstIndex, vertexOffset, 0);

                    firstIndex += model.meshes.get(0).indices.size();
                    vertexOffset += model.meshes.get(0).getVertices().size();
                }
            }
            vkCmdEndRenderPass(commandBuffer);


            if (vkEndCommandBuffer(commandBuffer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to record command buffer");
            }
        }

    }

    private void drawFrame() {

        try(MemoryStack stack = stackPush()) {

            Frame thisFrame = inFlightFrames.get(currentFrame);

            vkWaitForFences(device, thisFrame.pFence(), true, UINT64_MAX);

            IntBuffer pImageIndex = stack.mallocInt(1);

            int vkResult = vkAcquireNextImageKHR(device, swapChain, UINT64_MAX,
                    thisFrame.presentSemaphore(), VK_NULL_HANDLE, pImageIndex);

            if(vkResult == VK_ERROR_OUT_OF_DATE_KHR) {
                recreateSwapChain();
                return;
            } else if(vkResult != VK_SUCCESS) {
                throw new RuntimeException("Cannot get image");
            }

            final int imageIndex = pImageIndex.get(0);

            updateCameraGPUDataInMemory(currentFrame);
            updateSceneGPUDataInMemory(currentFrame);
            recordCommandBuffer(thisFrame, currentFrame, swapChainFramebuffers.get(imageIndex));

            if(imagesInFlight.containsKey(imageIndex)) {
                vkWaitForFences(device, imagesInFlight.get(imageIndex).fence(), true, UINT64_MAX);
            }
            imagesInFlight.put(imageIndex, thisFrame);
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

            currentFrame = (currentFrame + 1) % MAX_FRAMES_IN_FLIGHT;
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

    @Override
    public void cleanUp() {
        // Wait for the device to complete all operations before release resources
        display.cleanUp();
        vkDeviceWaitIdle(device);

        cleanupSwapChain();

        models.forEach(model -> model.meshes.get(0).materials.get(0).texture.cleanUp());

        vkDestroyDescriptorPool(device, descriptorPool, null);
        vkDestroyDescriptorSetLayout(device, globalDescriptorSetLayout, null);

        vkDestroyBuffer(device, vertexBuffer, null);
        vkFreeMemory(device, vertexBufferMemory, null);

        vkDestroyBuffer(device, indexBuffer, null);
        vkFreeMemory(device, indexBufferMemory, null);

        vkDestroyBuffer(device, gpuSceneDataBuffer, null);
        vkFreeMemory(device, gpUSceneDataBufferMemory, null);

        inFlightFrames.forEach(frame -> {
            vkDestroySemaphore(device, frame.renderFinishedSemaphore(), null);
            vkDestroySemaphore(device, frame.presentSemaphore(), null);
            vkDestroyFence(device, frame.fence(), null);
            vkDestroyCommandPool(device, frame.commandPool, null);
            vmaDestroyBuffer(vmaAllocator, frame.cameraBuffer.buffer, frame.cameraBuffer.allocation);
        });
        inFlightFrames.clear();

        vkDestroyCommandPool(device, globalCommandPool, null);

        vmaDestroyAllocator(vmaAllocator);

        vkDestroyDevice(device, null);

        if(ENABLE_VALIDATION_LAYERS) {
            if(vkGetInstanceProcAddr(instance, "vkDestroyDebugUtilsMessengerEXT") != NULL) {
                vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, null);
            }
        }

        vkDestroySurfaceKHR(instance, surface, null);

        vkDestroyInstance(instance, null);

        glfwDestroyWindow(display.window);

        glfwTerminate();

        log("cleanup Finished calling ");
    }

    private void initVulkan() {
        Vulkan.createInstance("Vulkan game", "Kurama Engine");
        Vulkan.setupDebugMessenger();

        surface = createSurface(instance, display.window);
        physicalDevice = pickPhysicalDevice(instance, surface, DEVICE_EXTENSIONS);
        createLogicalDevice();

        gpuProperties = getGPUProperties(physicalDevice);
        msaaSamples = getMaxUsableSampleCount(gpuProperties);
        minUniformBufferOffsetAlignment = getMinBufferOffsetAlignment(gpuProperties);

        vmaAllocator = createAllocator(physicalDevice, device, instance);

        initializeFrames();
        createFrameCommandPoolsAndBuffers();

        createGlobalCommandPool();
        createGlobalCommandBuffer();

        loadModels();

//        models.forEach(model -> {
//            ((TextureVK)model.meshes.get(0).materials.get(0).texture).createTextureImage(globalCommandPool, graphicsQueue);
//            ((TextureVK)model.meshes.get(0).materials.get(0).texture).createTextureImageView();
//            ((TextureVK)model.meshes.get(0).materials.get(0).texture).createTextureSampler();
//        });

        // All models share the same vertex and index buffer
        // Maybe this might have to be changed later, or use a more complex scene manager to handle dynamic loading and unloading of objects
        createVertexBuffer();
        createIndexBuffer();

        createSwapChain();
        createImageViews();

        createRenderPass();

        createColorResources();
        createDepthResources();
        createFramebuffers();

        createSemaphoresAndFences();

        // Descriptor set layout is needed when both defining the pipelines, and when creating the descriptor sets
        initDescriptors();
        createGraphicsPipeline();
    }

    public void initDescriptors() {

        try (var stack = stackPush()) {

            var pSceneBuffer = stack.mallocLong(1);
            var pSceneBufferMemory = stack.mallocLong(1);

            var sceneParamsBufferSize = MAX_FRAMES_IN_FLIGHT * padUniformBufferSize(GPUSceneData.SIZEOF, minUniformBufferOffsetAlignment);
            createBuffer(device, physicalDevice,
                    sceneParamsBufferSize,
                    VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                    pSceneBuffer,
                    pSceneBufferMemory
                    );
            gpuSceneDataBuffer = pSceneBuffer.get(0);
            gpUSceneDataBufferMemory = pSceneBufferMemory.get(0);

            // Creates a descriptor set layout with 2 bindings
            // binding 0 = GPU Camera Data
            // binding 1 = scene data
            createDescriptorSetLayout();

            // Allocate a descriptor pool that can create a max of 10 sets and 10 uniform buffers
            createDescriptorPool();

            for(int i = 0;i < inFlightFrames.size(); i++) {

//                var pBuffer = stack.mallocLong(1);
//                var pBufferMemory = stack.mallocLong(1);
                var pDescriptorSet = stack.mallocLong(1);

                // uniform buffer for GPU camera data
                // A camera buffer is created for each frame
                var cameraBuffer
                        = createBufferVMA(
                                vmaAllocator,
                                GPUCameraData.SIZEOF,
                                VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                                VMA_MEMORY_USAGE_CPU_TO_GPU);

                log("camera buffer buffer: "+ cameraBuffer.buffer);
                log("camera buffer allocation: "+ cameraBuffer.allocation);

//                createBuffer(device, physicalDevice,
//                        GPUCameraData.SIZEOF,
//                        VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
//                        VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
//                        pBuffer,
//                        pBufferMemory);
                inFlightFrames.get(i).cameraBuffer = cameraBuffer;

                // Create descriptor set per frame
                var layout = stack.mallocLong(1);
                layout.put(0, globalDescriptorSetLayout);

                // Allocate a descriptor set
                VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack);
                allocInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
                allocInfo.descriptorPool(descriptorPool);
                allocInfo.pSetLayouts(layout);

                if(vkAllocateDescriptorSets(device, allocInfo, pDescriptorSet) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to allocate descriptor sets");
                }
                inFlightFrames.get(i).globalDescriptorSet = pDescriptorSet.get(0);

                //information about the buffer we want to point at in the descriptor
                VkDescriptorBufferInfo.Buffer cameraBufferInfo = VkDescriptorBufferInfo.calloc(1, stack);
                cameraBufferInfo.offset(0);
                cameraBufferInfo.range(GPUCameraData.SIZEOF);
                cameraBufferInfo.buffer(inFlightFrames.get(i).cameraBuffer.buffer);

                log("min alignment: "+ minUniformBufferOffsetAlignment);
                log("padding: "+ padUniformBufferSize(GPUSceneData.SIZEOF, minUniformBufferOffsetAlignment));

                VkDescriptorBufferInfo.Buffer sceneBufferInfo = VkDescriptorBufferInfo.calloc(1, stack);
                sceneBufferInfo.offset(0);
                sceneBufferInfo.range(GPUSceneData.SIZEOF);
                sceneBufferInfo.buffer(gpuSceneDataBuffer);

                VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(2, stack);
                var gpuCameraWrite =
                        createWriteDescriptorSet(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER,
                                inFlightFrames.get(i).globalDescriptorSet,
                                cameraBufferInfo,
                                0, stack);

                var sceneWrite =
                        createWriteDescriptorSet(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC,
                                inFlightFrames.get(i).globalDescriptorSet,
                                sceneBufferInfo, 1, stack);

                descriptorWrites.put(0, gpuCameraWrite);
                descriptorWrites.put(1, sceneWrite);

                vkUpdateDescriptorSets(device, descriptorWrites, null);
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
        createImageViews();
        createRenderPass();
        createGraphicsPipeline();
        createColorResources();
        createDepthResources();
        createFramebuffers();

        createGlobalCommandBuffer();
        recreateFrameCommandBuffers();
    }

    private void cleanupSwapChain() {

        swapChainFramebuffers.forEach(framebuffer -> vkDestroyFramebuffer(device, framebuffer, null));

        vkFreeCommandBuffers(device, globalCommandPool, Vulkan.asPointerBuffer(List.of(new VkCommandBuffer[]{globalCommandBuffer})));

        vkDestroyPipeline(device, graphicsPipeline, null);

        vkDestroyPipelineLayout(device, pipelineLayout, null);

        vkDestroyRenderPass(device, renderPass, null);

        swapChainImageViews.forEach(imageView -> vkDestroyImageView(device, imageView, null));

        vkDestroyImageView(device, depthImageView, null);
        vkDestroyImage(device, depthImage, null);
        vkFreeMemory(device, depthImageMemory, null);

        vkDestroyImageView(device, colorImageView, null);
        vkDestroyImage(device, colorImage, null);
        vkFreeMemory(device, colorImageMemory, null);

        vkDestroySwapchainKHR(device, swapChain, null);
    }

    public void loadModels() {

        var location = "projects/VulkanTestProject/models/meshes/viking_room.obj";
        var textureDir = "projects/VulkanTestProject/models/textures/";

        List<Mesh> meshes;

        try {
            meshes = AssimpStaticLoader.load(location, textureDir);
            log("successfuly loaded meshes");
        }
        catch (Exception e) {
            logError("Failed to load mesh");
            throw new IllegalArgumentException("Could not load mesh");
        }

        //Add texture loc
        meshes.get(0).materials.get(0).texture = Texture.createTexture(textureDir + "viking_room.png");

        //add color just for the sake of consistency with vulkan tutorial
        var colors = new ArrayList<Vector>();
        meshes.get(0).getVertices().forEach(v -> colors.add(new Vector(new float[]{0.1f, 0.1f, 0.1f})));
        meshes.get(0).setAttribute(colors, Mesh.COLOR);
        var room = new Model(this, meshes, "room");

        List<Mesh> meshes2;
        try {
            meshes2 = AssimpStaticLoader.load("projects/VulkanTestProject/models/meshes/house.obj", textureDir);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        //Add texture loc
        meshes2.get(0).materials.get(0).texture = Texture.createTexture(textureDir + "house_text.jpg");

        //add color just for the sake of consistency with vulkan tutorial
        var colors2 = new ArrayList<Vector>();
        meshes2.get(0).getVertices().forEach(v -> colors2.add(new Vector(new float[]{1f,1f,1f})));
        meshes2.get(0).setAttribute(colors2, Mesh.COLOR);
        var house = new Model(this, meshes2, "house");
//        house.pos = new Vector(0,0,-2);
        house.setScale(1f);
//        house.setOrientation(Quaternion.getQuaternionFromEuler(-90, 0,0));

        models.add(room);
//        models.add(house);
    }

    private void createDepthResources() {
        try(MemoryStack stack = stackPush()) {

            int depthFormat = findDepthFormat();

            LongBuffer pDepthImage = stack.mallocLong(1);
            LongBuffer pDepthImageMemory = stack.mallocLong(1);

            createImage(swapChainExtent.width(), swapChainExtent.height(),
                    depthFormat,
                    VK_IMAGE_TILING_OPTIMAL,
                    VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT,
                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                    1,
                    msaaSamples,
                    pDepthImage, pDepthImageMemory);

            depthImage = pDepthImage.get(0);
            depthImageMemory = pDepthImageMemory.get(0);

            depthImageView = createImageView(depthImage, depthFormat, VK_IMAGE_ASPECT_DEPTH_BIT, 1, device);
            // Explicitly transitioning the depth image
            transitionImageLayout(depthImage, depthFormat,
                    VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL, 1, globalCommandPool, graphicsQueue);

        }
    }

    private int findDepthFormat() {
        return findSupportedFormat(
                physicalDevice,
                stackGet().ints(VK_FORMAT_D32_SFLOAT, VK_FORMAT_D32_SFLOAT_S8_UINT, VK_FORMAT_D24_UNORM_S8_UINT),
                VK_IMAGE_TILING_OPTIMAL,
                VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT);
    }

    private void createSwapChain() {

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

            swapChainImages = new ArrayList<>(imageCount.get(0));

            for(int i = 0;i < pSwapchainImages.capacity();i++) {
                swapChainImages.add(pSwapchainImages.get(i));
            }

            swapChainImageFormat = surfaceFormat.format();
            swapChainExtent = VkExtent2D.create().set(extent);
        }
    }

    private void createImageViews() {
        swapChainImageViews = new ArrayList<>(swapChainImages.size());
        for(long swapChainImage : swapChainImages) {
            swapChainImageViews.add(createImageView(swapChainImage, swapChainImageFormat, VK_IMAGE_ASPECT_COLOR_BIT, 1, device));
        }
    }

    private void createRenderPass() {

        try(MemoryStack stack = stackPush()) {

            var attachments  = VkAttachmentDescription.calloc(3, stack);
            var attachmentRefs = VkAttachmentReference.calloc(3, stack);

            // MSA image
            var colorAttachment = attachments .get(0);
            colorAttachment.format(swapChainImageFormat);
            colorAttachment.samples(msaaSamples);
            colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
            colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
            colorAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            colorAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            colorAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            colorAttachment.finalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

           var colorAttachmentRef = attachmentRefs.get(0);
            colorAttachmentRef.attachment(0);
            colorAttachmentRef.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            // Present image attachment
            var colorAttachmentResolve = attachments.get(2);
            colorAttachmentResolve.format(swapChainImageFormat);
            colorAttachmentResolve.samples(VK_SAMPLE_COUNT_1_BIT);
            colorAttachmentResolve.loadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            colorAttachmentResolve.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
            colorAttachmentResolve.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            colorAttachmentResolve.stencilStoreOp(VK_ATTACHMENT_STORE_OP_STORE);
            colorAttachmentResolve.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            colorAttachmentResolve.finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

            var colorAttachmentResolveRef = attachmentRefs.get(2);
            colorAttachmentResolveRef.attachment(2);
            colorAttachmentResolveRef.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            // Depth-Stencil attachments
            VkAttachmentDescription depthAttachment = attachments.get(1);
            depthAttachment.format(findDepthFormat());
            depthAttachment.samples(msaaSamples);
            depthAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
            depthAttachment.storeOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            depthAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            depthAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            depthAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            depthAttachment.finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

            VkAttachmentReference depthAttachmentRef = attachmentRefs.get(1);
            depthAttachmentRef.attachment(1);
            depthAttachmentRef.layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

            VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1, stack);
            subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);
            subpass.colorAttachmentCount(1);
            subpass.pResolveAttachments(VkAttachmentReference.calloc(1, stack).put(0, colorAttachmentResolveRef));
            subpass.pColorAttachments(VkAttachmentReference.calloc(1, stack).put(0, colorAttachmentRef));
            subpass.pDepthStencilAttachment(depthAttachmentRef);

            VkSubpassDependency.Buffer dependency = VkSubpassDependency.calloc(1, stack);
            dependency.srcSubpass(VK_SUBPASS_EXTERNAL);
            dependency.dstSubpass(0);
            dependency.srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT);
            dependency.srcAccessMask(0);
            dependency.dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT);
            dependency.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);

            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack);
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO);
            renderPassInfo.pAttachments(attachments);
            renderPassInfo.pSubpasses(subpass);
            renderPassInfo.pDependencies(dependency);

            LongBuffer pRenderPass = stack.mallocLong(1);

            if(vkCreateRenderPass(device, renderPassInfo, null, pRenderPass) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create render pass");
            }

            renderPass = pRenderPass.get(0);
        }
    }

    private void createGraphicsPipeline() {

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
            depthStencil.depthCompareOp(VK_COMPARE_OP_LESS);
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
            multisampling.rasterizationSamples(msaaSamples);
            multisampling.sampleShadingEnable(true);
            multisampling.minSampleShading(1f);

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
            pipelineLayoutInfo.pSetLayouts(stack.longs(globalDescriptorSetLayout));

            // Set push constants
            VkPushConstantRange.Buffer pushConstant = VkPushConstantRange.calloc(1, stack);
            pushConstant.offset(0);
            pushConstant.size(MeshPushConstants.SIZEOF);
            pushConstant.stageFlags(VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT);

            pipelineLayoutInfo.pPushConstantRanges(pushConstant);

            // ===> PIPELINE LAYOUT CREATION <===

            LongBuffer pPipelineLayout = stack.longs(VK_NULL_HANDLE);

            if(vkCreatePipelineLayout(device, pipelineLayoutInfo, null, pPipelineLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create pipeline layout");
            }

            pipelineLayout = pPipelineLayout.get(0);

            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack);
            pipelineInfo.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
            pipelineInfo.pStages(shaderStages);
            pipelineInfo.pVertexInputState(vertexInputInfo);
            pipelineInfo.pInputAssemblyState(inputAssembly);
            pipelineInfo.pViewportState(viewportState);
            pipelineInfo.pRasterizationState(rasterizer);
            pipelineInfo.pMultisampleState(multisampling);
            pipelineInfo.pColorBlendState(colorBlending);
            pipelineInfo.layout(pipelineLayout);
            pipelineInfo.renderPass(renderPass);
            pipelineInfo.subpass(0);
            pipelineInfo.basePipelineHandle(VK_NULL_HANDLE);
            pipelineInfo.basePipelineIndex(-1);
            pipelineInfo.pDepthStencilState(depthStencil);

            LongBuffer pGraphicsPipeline = stack.mallocLong(1);

            if(vkCreateGraphicsPipelines(device, VK_NULL_HANDLE, pipelineInfo, null, pGraphicsPipeline) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create graphics pipeline");
            }

            graphicsPipeline = pGraphicsPipeline.get(0);

            // ===> RELEASE RESOURCES <===

            vkDestroyShaderModule(device, vertShaderModule, null);
            vkDestroyShaderModule(device, fragShaderModule, null);

            vertShaderSPIRV.free();
            fragShaderSPIRV.free();
        }
    }

    private void createFramebuffers() {

        swapChainFramebuffers = new ArrayList<>(swapChainImageViews.size());

        try(MemoryStack stack = stackPush()) {

            LongBuffer attachments = stack.longs(colorImageView, depthImageView, VK_NULL_HANDLE);
            LongBuffer pFramebuffer = stack.mallocLong(1);

            // Lets allocate the create info struct once and just update the pAttachments field each iteration
            VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.calloc(stack);
            framebufferInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
            framebufferInfo.renderPass(renderPass);
            framebufferInfo.width(swapChainExtent.width());
            framebufferInfo.height(swapChainExtent.height());
            framebufferInfo.layers(1);

            for(long imageView : swapChainImageViews) {

                attachments.put(2, imageView);

                framebufferInfo.pAttachments(attachments);

                if(vkCreateFramebuffer(device, framebufferInfo, null, pFramebuffer) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create framebuffer");
                }

                swapChainFramebuffers.add(pFramebuffer.get(0));
            }
        }
    }

    private void createGlobalCommandPool() {

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

        }
    }

    private void createLogicalDevice() {
        try(MemoryStack stack = stackPush()) {

            QueueFamilyIndices indices = Vulkan.findQueueFamilies(physicalDevice, surface);

            int[] uniqueQueueFamilies = indices.unique();

            VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.calloc(uniqueQueueFamilies.length, stack);

            for(int i = 0;i < uniqueQueueFamilies.length;i++) {
                VkDeviceQueueCreateInfo queueCreateInfo = queueCreateInfos.get(i);
                queueCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
                queueCreateInfo.queueFamilyIndex(uniqueQueueFamilies[i]);
                queueCreateInfo.pQueuePriorities(stack.floats(1.0f));
            }

            VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.calloc(stack);
            deviceFeatures.samplerAnisotropy(true);
            deviceFeatures.sampleRateShading(true);

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
            createInfo.pQueueCreateInfos(queueCreateInfos);
            // queueCreateInfoCount is automatically set

            createInfo.pEnabledFeatures(deviceFeatures);

            createInfo.ppEnabledExtensionNames(Vulkan.asPointerBuffer(DEVICE_EXTENSIONS));

            if(ENABLE_VALIDATION_LAYERS) {
                createInfo.ppEnabledLayerNames(Vulkan.asPointerBuffer(VALIDATION_LAYERS));
            }

            PointerBuffer pDevice = stack.pointers(VK_NULL_HANDLE);

            if(vkCreateDevice(physicalDevice, createInfo, null, pDevice) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create logical device");
            }

            device = new VkDevice(pDevice.get(0), physicalDevice, createInfo);

            PointerBuffer pQueue = stack.pointers(VK_NULL_HANDLE);

            vkGetDeviceQueue(device, indices.graphicsFamily, 0, pQueue);
            graphicsQueue = new VkQueue(pQueue.get(0), device);

            vkGetDeviceQueue(device, indices.presentFamily, 0, pQueue);
            presentQueue = new VkQueue(pQueue.get(0), device);
        }
    }

    public void createDescriptorSetLayout() {
        try (var stack = stackPush()) {

            var cameraBufferBinding =
                    createDescriptorSetLayoutBinding(
                    0, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, VK_SHADER_STAGE_VERTEX_BIT, stack);

            var sceneBinding =
                    createDescriptorSetLayoutBinding(
                            1, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC,
                            VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT, stack);

            VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(2, stack);
            bindings.put(0, cameraBufferBinding);
            bindings.put(1, sceneBinding);

            VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack);
            layoutInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
            layoutInfo.pBindings(bindings);

            LongBuffer pDescriptorSetLayout = stack.mallocLong(1);

            if(vkCreateDescriptorSetLayout(device, layoutInfo, null, pDescriptorSetLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor set layout");
            }
            globalDescriptorSetLayout = pDescriptorSetLayout.get(0);
        }
    }

    private void createIndexBuffer() {
        try (var stack = stackPush()) {
            long bufferSize = 0;
            for(var model: models) {
                for(var mesh: model.meshes) {
                    bufferSize += Short.SIZE * mesh.indices.size();
                }
            }

            var pBuffer = stack.mallocLong(1);
            var pBufferMemory = stack.mallocLong(1);
            createBuffer(device, physicalDevice, bufferSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                    pBuffer, pBufferMemory);

            long stagingBuffer = pBuffer.get(0);
            long stagingBufferMemory = pBufferMemory.get(0);

            var data = stack.mallocPointer(1);

            vkMapMemory(device, stagingBufferMemory, 0, bufferSize, 0, data);
            {
                for(var model: models) {
                    for(var mesh: model.meshes) {
                        memcpyInt(data.getByteBuffer(0, (int) bufferSize),mesh.indices);
                    }
                }
            }
            vkUnmapMemory(device, stagingBufferMemory);

            createBuffer(device, physicalDevice, bufferSize,
                    VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
                    VK_MEMORY_HEAP_DEVICE_LOCAL_BIT,
                    pBuffer,
                    pBufferMemory);

            indexBuffer = pBuffer.get(0);
            indexBufferMemory = pBufferMemory.get(0);

            copyBuffer(device, globalCommandPool, graphicsQueue, stagingBuffer, indexBuffer, bufferSize);

            vkDestroyBuffer(device, stagingBuffer, null);
            vkFreeMemory(device, stagingBufferMemory, null);
        }
    }

    private void createVertexBuffer() {
        try (var stack = stackPush()) {
            long bufferSize = 0;
            for(var model: models) {
                for(var mesh: model.meshes) {
                    bufferSize += Vertex.SIZEOF * mesh.getVertices().size();
                }
            }

            var pBuffer = stack.mallocLong(1);
            var pBufferMemory = stack.mallocLong(1);
            createBuffer(device, physicalDevice, bufferSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                    pBuffer, pBufferMemory);

            long stagingBuffer = pBuffer.get(0);
            long stagingBufferMemory = pBufferMemory.get(0);

            var data = stack.mallocPointer(1);

            vkMapMemory(device, stagingBufferMemory, 0, bufferSize, 0, data);
            {
                for(var model: models) {
                    for(var mesh: model.meshes) {
                        memcpy(data.getByteBuffer(0, (int) bufferSize), mesh);
                    }
                }
            }
            vkUnmapMemory(device, stagingBufferMemory);

            createBuffer(device, physicalDevice, bufferSize,
                    VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                    VK_MEMORY_HEAP_DEVICE_LOCAL_BIT,
                    pBuffer,
                    pBufferMemory);

            vertexBuffer = pBuffer.get(0);
            vertexBufferMemory = pBufferMemory.get(0);

            copyBuffer(device, globalCommandPool, graphicsQueue, stagingBuffer, vertexBuffer, bufferSize);

            vkDestroyBuffer(device, stagingBuffer, null);
            vkFreeMemory(device, stagingBufferMemory, null);
        }
    }

    private void createColorResources() {
        try(var stack = stackPush()) {

            LongBuffer pColorImage = stack.mallocLong(1);
            LongBuffer pColorImageMemory = stack.mallocLong(1);

            createImage(swapChainExtent.width(), swapChainExtent.height(),
                    swapChainImageFormat,
                    VK_IMAGE_TILING_OPTIMAL,
                    VK_IMAGE_USAGE_TRANSIENT_ATTACHMENT_BIT | VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT,
                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                    1,
                    msaaSamples,
                    pColorImage,
                    pColorImageMemory);

            colorImage = pColorImage.get(0);
            colorImageMemory = pColorImageMemory.get(0);

            colorImageView = createImageView(colorImage, swapChainImageFormat, VK_IMAGE_ASPECT_COLOR_BIT, 1, device);

            transitionImageLayout(colorImage, swapChainImageFormat, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, 1, globalCommandPool, graphicsQueue);
        }
    }

    // Allocate a descriptor pool that can create a max of 10 sets and 10 uniform buffers
    private void createDescriptorPool() {
        try (var stack = stackPush()) {

            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(2, stack);

            VkDescriptorPoolSize uniformBufferPoolSize  = poolSizes.get(0);
            uniformBufferPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
            uniformBufferPoolSize.descriptorCount(10);

            VkDescriptorPoolSize uniformBufferDynamicPoolSize  = poolSizes.get(1);
            uniformBufferDynamicPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC);
            uniformBufferDynamicPoolSize.descriptorCount(10);

//            VkDescriptorPoolSize textureSamplerPoolSize = poolSizes.get(1);
//            textureSamplerPoolSize.type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
//            textureSamplerPoolSize.descriptorCount(inFlightFrames.size());

            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
            poolInfo.pPoolSizes(poolSizes);
            poolInfo.maxSets(10);

            LongBuffer pDescriptorPool = stack.mallocLong(1);

            if(vkCreateDescriptorPool(device, poolInfo, null, pDescriptorPool) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor pool");
            }

            descriptorPool = pDescriptorPool.get(0);
        }
    }

    private void updateCameraGPUDataInMemory(int currentFrame) {
        try(MemoryStack stack = stackPush()) {

            PointerBuffer data = stack.mallocPointer(1);
            vmaMapMemory(vmaAllocator, inFlightFrames.get(currentFrame).cameraBuffer.allocation, data);
            {
                memcpy(data.getByteBuffer(0, GPUCameraData.SIZEOF), gpuCameraData);
            }
            vmaUnmapMemory(vmaAllocator, inFlightFrames.get(currentFrame).cameraBuffer.allocation);
        }
    }

    private void updateSceneGPUDataInMemory(int currentFrame) {
        try(MemoryStack stack = stackPush()) {

            PointerBuffer data = stack.mallocPointer(1);

            // Mapping the buffer with the right offset depending on the current frame
            vkMapMemory(device, gpUSceneDataBufferMemory,
                    padUniformBufferSize(GPUSceneData.SIZEOF, minUniformBufferOffsetAlignment) * currentFrame,
                    GPUSceneData.SIZEOF, 0, data);
            {
                memcpy(data.getByteBuffer(0, GPUSceneData.SIZEOF), gpuSceneData);
            }
            vkUnmapMemory(device, gpUSceneDataBufferMemory);
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
                inFlightFrames.get(i).commandBuffer = commandBuffer;
            }
        }
    }

    public void initializeFrames() {
        inFlightFrames = new ArrayList<>(MAX_FRAMES_IN_FLIGHT);

        for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
            inFlightFrames.add(new Frame());
        }
    }

    public void createFrameCommandPoolsAndBuffers() {

        try (var stack = stackPush()) {
            for (int i = 0; i < inFlightFrames.size(); i++) {

                // Create command pool
                QueueFamilyIndices queueFamilyIndices = findQueueFamilies(physicalDevice, surface);

                VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack);
                poolInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
                poolInfo.queueFamilyIndex(queueFamilyIndices.graphicsFamily);
                poolInfo.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);

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
                if(vkAllocateCommandBuffers(device, allocInfo, pCommandBuffers) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to allocate command buffers");
                }

                var commandBuffer = new VkCommandBuffer(pCommandBuffers.get(0), device);

                inFlightFrames.get(i).commandPool = commandPool;
                inFlightFrames.get(i).commandBuffer = commandBuffer;
            }
        }
    }

    public void createSemaphoresAndFences() {

        imagesInFlight = new HashMap<>(swapChainImages.size());

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

                inFlightFrames.get(i).imageAvailableSemaphore = pImageAvailableSemaphore.get(0);
                inFlightFrames.get(i).renderFinishedSemaphore = pRenderFinishedSemaphore.get(0);
                inFlightFrames.get(i).fence = pFence.get(0);
            }

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
            FloatBuffer res = MemoryUtil.memAllocFloat((4 * 4 * 4) + (4 * 4));

            for(float v: data.getData()) {
                res.put(v);
            }

            for(Vector c:renderMatrix.convertToColumnVectorArray()) {
                for(float val: c.getData()) {
                    res.put(val);
                }
            }
            res.flip();
            return res;
        }
    }

    public class GPUCameraData {

        public static final int SIZEOF = 3 * 16 * Float.BYTES;

        public Matrix projview;
        public Matrix view;
        public Matrix proj;

        public GPUCameraData() {
            projview = Matrix.getIdentityMatrix(4);
            view = Matrix.getIdentityMatrix(4);
            proj = Matrix.getIdentityMatrix(4);
        }

    }

    public class GPUSceneData {
        public static final int SIZEOF = Float.BYTES * 4 * 5;
        public Vector fogColor; // w is for exponent
        public Vector fogDistance; //x for min, y for max, zw unused.
        public Vector ambientColor;
        public Vector sunlightDirection; // w for sun power
        public Vector sunLightColor;
    }

}
