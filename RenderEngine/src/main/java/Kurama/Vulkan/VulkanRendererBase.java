package Kurama.Vulkan;

import Kurama.display.DisplayVulkan;
import Kurama.game.Game;
import Kurama.renderingEngine.RenderingEngine;
import Kurama.scene.Scene;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static Kurama.Vulkan.VulkanUtilities.*;
import static Kurama.Vulkan.VulkanUtilities.deletionQueue;
import static Kurama.utils.Logger.log;
import static java.util.stream.Collectors.toSet;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFW.glfwWaitEvents;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.util.vma.Vma.vmaDestroyBuffer;
import static org.lwjgl.vulkan.KHRMultiview.VK_KHR_MULTIVIEW_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.vkCmdCopyBuffer;
import static org.lwjgl.vulkan.VK12.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_1_FEATURES;
import static org.lwjgl.vulkan.VK12.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES;

public abstract class VulkanRendererBase extends RenderingEngine {

    public DisplayVulkan display;
    public long vmaAllocator;
    public long surface;
    public int numOfSamples = VK_SAMPLE_COUNT_1_BIT;
    public long minUniformBufferOffsetAlignment = 64;
    public VkPhysicalDeviceProperties gpuProperties;
    public QueueFamilyIndices queueFamilyIndices;
    public VkQueue graphicsQueue;
    public VkQueue presentQueue;
    public VkQueue computeQueue;
    public VkQueue transferQueue;
    public long swapChain;
    public List<SwapChainAttachment> swapChainAttachments;
    public int swapChainImageFormat;
    public VkExtent2D swapChainExtent;
    public DescriptorAllocator descriptorAllocator = new DescriptorAllocator();
    public DescriptorSetLayoutCache descriptorSetLayoutCache = new DescriptorSetLayoutCache();
    public boolean msaaEnabled = false;
    public Set<String> DEVICE_EXTENSIONS =
            Stream.of(
                            VK_KHR_SWAPCHAIN_EXTENSION_NAME,
                            VK_KHR_MULTIVIEW_EXTENSION_NAME)
                    .collect(toSet());
    public List<Renderable> renderables = new ArrayList<>();
    public AllocatedBuffer mergedVertexBuffer;
    public AllocatedBuffer mergedIndexBuffer;
    public SingleTimeCommandContext singleTimeGraphicsCommandContext;
    public SingleTimeCommandContext singleTimeTransferCommandContext;
    public SingleTimeCommandContext singleTimeComputeCommandContext;
    public HashMap<String, TextureVK> preparedTextures = new HashMap<>();;
    public HashMap<Integer, Long> textureSamplerToMaxLOD = new HashMap<>();
    public HashMap<String, Long> textureFileToDescriptorSet = new HashMap<>();

    public VulkanRendererBase(Game game) {
        super(game);
    }

    public abstract void initRenderer();
    // Event callback when swapchain is recreated
    public abstract void swapChainRecreatedEvent();

    // Event callback
    public abstract void meshesMergedEvent();

    public abstract void initDescriptorSets();
    public abstract void initBuffers();

    public abstract void render();

    @Override
    public void init(Scene scene) {
        this.display = (DisplayVulkan) game.display;

        VulkanUtilities.createInstance(game.name, game.name);
        VulkanUtilities.setupDebugMessenger();

        surface = createSurface(instance, display.window);
        physicalDevice = pickPhysicalDevice(instance, surface, DEVICE_EXTENSIONS);
        createLogicalDevice();

        descriptorAllocator.init(device);
        descriptorSetLayoutCache.init(device);

        deletionQueue.add(() -> descriptorSetLayoutCache.cleanUp());
        deletionQueue.add(() -> descriptorAllocator.cleanUp());

        gpuProperties = getGPUProperties(physicalDevice);

        minUniformBufferOffsetAlignment = getMinBufferOffsetAlignment(gpuProperties);

        vmaAllocator = createAllocator(physicalDevice, device, instance);

        initRenderer();
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
            if(deviceFeatures.multiDrawIndirect()) {
                deviceFeatures.multiDrawIndirect(true);
            }

            var vkPhysicalDeviceVulkan11Features = VkPhysicalDeviceVulkan11Features.calloc(stack);
            vkPhysicalDeviceVulkan11Features.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_1_FEATURES);
            vkPhysicalDeviceVulkan11Features.shaderDrawParameters(true);
            vkPhysicalDeviceVulkan11Features.multiview(true);

            var physicalDevice12Features = VkPhysicalDeviceVulkan12Features.calloc(stack);
            physicalDevice12Features.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES);
            physicalDevice12Features.timelineSemaphore(true);

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
            createInfo.pQueueCreateInfos(queueCreateInfos);
            createInfo.pNext(vkPhysicalDeviceVulkan11Features);
            createInfo.pNext(physicalDevice12Features);
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

            swapChainAttachments = new ArrayList<>(imageCount.get(0));

            for(int i = 0;i < pSwapchainImages.capacity();i++) {
                var swapImage = new SwapChainAttachment();
                swapImage.swapChainImage = pSwapchainImages.get(i);
                swapChainAttachments.add(swapImage);
            }

            swapChainImageFormat = surfaceFormat.format();
            swapChainExtent = VkExtent2D.create().set(extent);
        }
    }

    public void createSwapChainImageViews() {
        if(swapChainAttachments == null) swapChainAttachments = new ArrayList<>();

        try (var stack = stackPush()) {

            for (var swapChainImageAttachment : swapChainAttachments) {
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

    public void generateMeshBuffers() {
        int totalVertices = 0;
        int totalIndices = 0;

        for(var r: renderables) {
            r.firstVertex = totalVertices;
            r.firstIndex = totalIndices;

            totalVertices += r.mesh.getVertices().size();
            totalIndices += r.mesh.indices.size();
        }

        var indexBufferSize = totalIndices * Integer.BYTES;
        var indexStagingBuffer = createBufferVMA(vmaAllocator,
                indexBufferSize,
                VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                VMA_MEMORY_USAGE_AUTO,
                VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT);

        var vertexBufferSize = totalVertices * (3 + 2 + 3) * Float.BYTES;
        var  vertexStagingBuffer = createBufferVMA(vmaAllocator,
                vertexBufferSize,
                VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                VMA_MEMORY_USAGE_AUTO,
                VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT);

        try (var stack = stackPush()) {

            var data = stack.mallocPointer(1);

            vmaMapMemory(vmaAllocator, indexStagingBuffer.allocation, data);
            var byteBuffer = data.getByteBuffer(0, indexBufferSize);

            for(int i = 0; i < renderables.size(); i++) {
                memcpyInt(byteBuffer, renderables.get(i).mesh.indices);
            }
            vmaUnmapMemory(vmaAllocator, indexStagingBuffer.allocation);

            vmaMapMemory(vmaAllocator, vertexStagingBuffer.allocation, data);
            byteBuffer = data.getByteBuffer(0, vertexBufferSize);

            for(int i = 0; i < renderables.size(); i++) {
                memcpy(byteBuffer, renderables.get(i).mesh);
            }
            vmaUnmapMemory(vmaAllocator, vertexStagingBuffer.allocation);


            mergedVertexBuffer = createBufferVMA(vmaAllocator, totalVertices * (3 + 2 + 3) * Float.BYTES,
                    VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                    VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE, VMA_ALLOCATION_CREATE_DEDICATED_MEMORY_BIT);

            mergedIndexBuffer = createBufferVMA(vmaAllocator, totalIndices * Integer.BYTES,
                    VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
                    VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE, VMA_ALLOCATION_CREATE_DEDICATED_MEMORY_BIT);

            var vertexCopy = VkBufferCopy.calloc(1, stack);
            var indexCopy = VkBufferCopy.calloc(1, stack);

            Consumer<VkCommandBuffer> copyCmd = cmd -> {
//                for(var r: renderables) {
                vertexCopy.dstOffset(0);
                vertexCopy.size(vertexBufferSize);
                vertexCopy.srcOffset(0);
                vkCmdCopyBuffer(cmd, vertexStagingBuffer.buffer, mergedVertexBuffer.buffer, vertexCopy);

                indexCopy.dstOffset(0);
                indexCopy.size(indexBufferSize);
                indexCopy.srcOffset(0);
                vkCmdCopyBuffer(cmd, indexStagingBuffer.buffer, mergedIndexBuffer.buffer, indexCopy);
//                }
            };

            submitImmediateCommand(copyCmd, singleTimeTransferCommandContext);
        }

        vmaDestroyBuffer(vmaAllocator, vertexStagingBuffer.buffer, vertexStagingBuffer.allocation);
        vmaDestroyBuffer(vmaAllocator, indexStagingBuffer.buffer, indexStagingBuffer.allocation);

        meshesMergedEvent();
        deletionQueue.add(() -> vmaDestroyBuffer(vmaAllocator, mergedVertexBuffer.buffer, mergedVertexBuffer.allocation));
        deletionQueue.add(() -> vmaDestroyBuffer(vmaAllocator, mergedIndexBuffer.buffer, mergedIndexBuffer.allocation));
    }

    public long getTextureSampler(int maxLod) {
        textureSamplerToMaxLOD.computeIfAbsent(maxLod, TextureVK::createTextureSampler);
        return textureSamplerToMaxLOD.get(maxLod);
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

    public void prepareTexture(TextureVK texture) {
        if (texture == null || texture.fileName == null || preparedTextures.containsKey(texture.fileName)) return;

        TextureVK.createTextureImage(vmaAllocator, singleTimeGraphicsCommandContext, texture);
        TextureVK.createTextureImageView(texture);
        texture.textureSampler = getTextureSampler(texture.mipLevels);

        preparedTextures.put(texture.fileName, texture);
    }

    public void presentFrame(LongBuffer waitSemaphores, int imageIndex) {
        try (var stack = stackPush()) {
            // Display rendered image to screen
            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack);
            presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);
            presentInfo.pWaitSemaphores(waitSemaphores);
            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(stack.longs(swapChain));
            presentInfo.pImageIndices(stack.ints(imageIndex));

            int vkResult = vkQueuePresentKHR(presentQueue, presentInfo);

            if (vkResult == VK_ERROR_OUT_OF_DATE_KHR || vkResult == VK_SUBOPTIMAL_KHR) {
                recreateSwapChain();
            } else if (vkResult != VK_SUCCESS) {
                throw new RuntimeException("Failed to present swap chain image");
            }
        }
    }

    public Integer prepareDisplay(long signalSemaphore) {
        try(var stack = stackPush()) {

            IntBuffer pImageIndex = stack.mallocInt(1);
            int vkResult = vkAcquireNextImageKHR(device, swapChain, UINT64_MAX,
                    signalSemaphore, VK_NULL_HANDLE, pImageIndex);

            if(vkResult == VK_ERROR_OUT_OF_DATE_KHR) {
                recreateSwapChain();
                return null;
            } else if(vkResult != VK_SUCCESS) {
                throw new RuntimeException("Cannot get image");
            }

            return pImageIndex.get(0);

        }
    }

    public void recreateSwapChain() {

        try(MemoryStack stack = stackPush()) {

            IntBuffer width = stack.ints(0);
            IntBuffer height = stack.ints(0);

            while(width.get(0) == 0 && height.get(0) == 0) {
                glfwGetFramebufferSize(display.window, width, height);
                glfwWaitEvents();
            }
        }

        vkDeviceWaitIdle(device);
        swapChainRecreatedEvent();
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
