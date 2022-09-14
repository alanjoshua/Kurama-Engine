package Kurama.Vulkan;

import Kurama.Mesh.Mesh;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Pointer;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.util.vma.VmaAllocatorCreateInfo;
import org.lwjgl.util.vma.VmaVulkanFunctions;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static Kurama.utils.Logger.log;
import static java.util.stream.Collectors.toSet;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.Configuration.DEBUG;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK13.VK_API_VERSION_1_3;

public class VulkanUtilities {

    public static final int UINT32_MAX = 0xFFFFFFFF;
    public static final long UINT64_MAX = 0xFFFFFFFFFFFFFFFFL;
    public static VkDevice device;
    public static VkPhysicalDevice physicalDevice;
    public static VkInstance instance;
    public static final boolean ENABLE_VALIDATION_LAYERS = DEBUG.get(true);
    public static long debugMessenger;

    public static List<Runnable> deletionQueue = new ArrayList<>();

    public static final Set<String> VALIDATION_LAYERS;
    static {
        if(ENABLE_VALIDATION_LAYERS) {
            VALIDATION_LAYERS = new HashSet<>();
            VALIDATION_LAYERS.add("VK_LAYER_KHRONOS_validation");
        } else {
            // We are not going to use it, so we don't create it
            VALIDATION_LAYERS = null;
        }
    }

    public static void setupDebugMessenger() {

        if(!ENABLE_VALIDATION_LAYERS) {
            return;
        }

        try(MemoryStack stack = stackPush()) {

            VkDebugUtilsMessengerCreateInfoEXT createInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);

            populateDebugMessengerCreateInfo(createInfo);

            LongBuffer pDebugMessenger = stack.longs(VK_NULL_HANDLE);

            if(createDebugUtilsMessengerEXT(instance, createInfo, null, pDebugMessenger) != VK_SUCCESS) {
                throw new RuntimeException("Failed to set up debug messenger");
            }

            debugMessenger = pDebugMessenger.get(0);

            deletionQueue.add(() -> vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, null));
        }
    }

    private static int createDebugUtilsMessengerEXT(VkInstance instance, VkDebugUtilsMessengerCreateInfoEXT createInfo,
                                                    VkAllocationCallbacks allocationCallbacks, LongBuffer pDebugMessenger) {

        if(vkGetInstanceProcAddr(instance, "vkCreateDebugUtilsMessengerEXT") != NULL) {
            return vkCreateDebugUtilsMessengerEXT(instance, createInfo, allocationCallbacks, pDebugMessenger);
        }

        return VK_ERROR_EXTENSION_NOT_PRESENT;
    }

    public static void createInstance(String applicationName, String engineName) {

        if(ENABLE_VALIDATION_LAYERS && !checkValidationLayerSupport(VALIDATION_LAYERS)) {
            throw new RuntimeException("Validation requested but not supported");
        }

        try(MemoryStack stack = stackPush()) {

            // Use calloc to initialize the structs with 0s. Otherwise, the program can crash due to random values
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack);

            appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO);
            appInfo.pApplicationName(stack.UTF8Safe(applicationName));
            appInfo.applicationVersion(VK_MAKE_VERSION(1, 0, 0));
            appInfo.pEngineName(stack.UTF8Safe(engineName));
            appInfo.engineVersion(VK_MAKE_VERSION(1, 0, 0));
            appInfo.apiVersion(VK_API_VERSION_1_3);

            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
            createInfo.pApplicationInfo(appInfo);
            // enabledExtensionCount is implicitly set when you call ppEnabledExtensionNames
            createInfo.ppEnabledExtensionNames(getRequiredExtensions(ENABLE_VALIDATION_LAYERS));

            if(ENABLE_VALIDATION_LAYERS) {
                createInfo.ppEnabledLayerNames(asPointerBuffer(VALIDATION_LAYERS));
                VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);
                VulkanUtilities.populateDebugMessengerCreateInfo(debugCreateInfo);
                createInfo.pNext(debugCreateInfo.address());
            }

            // We need to retrieve the pointer of the created instance
            PointerBuffer instancePtr = stack.mallocPointer(1);

            if(vkCreateInstance(createInfo, null, instancePtr) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create instance");
            }

            instance = new VkInstance(instancePtr.get(0), createInfo);

            deletionQueue.add(() -> vkDestroyInstance(instance, null));
        }
    }

    private static void populateDebugMessengerCreateInfo(VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo) {
        debugCreateInfo.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT);
        debugCreateInfo.messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT);
        debugCreateInfo.messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT);
        debugCreateInfo.pfnUserCallback(VulkanUtilities::debugCallback);
    }

    private static int debugCallback(int messageSeverity, int messageType, long pCallbackData, long pUserData) {
        VkDebugUtilsMessengerCallbackDataEXT callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
        System.err.println("Validation layer: " + callbackData.pMessageString());
        return VK_FALSE;
    }

    public static long createShaderModule(ByteBuffer spirvCode, VkDevice device) {

        try(MemoryStack stack = stackPush()) {

            VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
            createInfo.pCode(spirvCode);

            LongBuffer pShaderModule = stack.mallocLong(1);

            if(vkCreateShaderModule(device, createInfo, null, pShaderModule) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create shader module");
            }

            return pShaderModule.get(0);
        }
    }

    public static AllocatedImage createImage(VkImageCreateInfo imageInfo, VmaAllocationCreateInfo allocInfo, long vmaAllocator) {

        try (var stack = stackPush()) {
            var pImage = stack.mallocLong(1);
            var pAllocation = stack.mallocPointer(1);

            vmaCreateImage(vmaAllocator, imageInfo, allocInfo, pImage, pAllocation, null);

            return new AllocatedImage(pImage.get(0), pAllocation.get(0));
        }
    }

    public static void transitionImageLayout(long image, int format, int oldLayout, int newLayout, int mipLevels, int layerCount, VkCommandBuffer commandBuffer) {
        try(MemoryStack stack = stackPush()) {
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack);
            barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
            barrier.oldLayout(oldLayout);
            barrier.newLayout(newLayout);
            barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.image(image);

            barrier.subresourceRange().baseMipLevel(0);
            barrier.subresourceRange().levelCount(mipLevels);
            barrier.subresourceRange().baseArrayLayer(0);
            barrier.subresourceRange().layerCount(layerCount);

            if(newLayout == VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL) {

                barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT);

                if(hasStencilComponent(format)) {
                    barrier.subresourceRange().aspectMask(
                            barrier.subresourceRange().aspectMask() | VK_IMAGE_ASPECT_STENCIL_BIT);
                }

            } else {
                barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            }

            int sourceStage;
            int destinationStage;

            if(oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {

                barrier.srcAccessMask(0);
                barrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);

                sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                destinationStage = VK_PIPELINE_STAGE_TRANSFER_BIT;

            } else if(oldLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL && newLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {

                barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);

                sourceStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
                destinationStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;

            } else if (oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL) {
                barrier.srcAccessMask(0);
                barrier.dstAccessMask(VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);

                sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                destinationStage = VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;

            } else if(oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL) {

                barrier.srcAccessMask(0);
                barrier.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);

                sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                destinationStage = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;

            }
            else {
                throw new IllegalArgumentException("Unsupported layout transition");
            }

            vkCmdPipelineBarrier(commandBuffer,
                    sourceStage, destinationStage,
                    0,
                    null,
                    null,
                    barrier);

        }
    }

    public static VkSurfaceFormatKHR chooseSwapSurfaceFormat(VkSurfaceFormatKHR.Buffer availableFormats) {
        return availableFormats.stream()
                .filter(availableFormat -> availableFormat.format() == VK_FORMAT_B8G8R8_SRGB)
                .filter(availableFormat -> availableFormat.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                .findAny()
                .orElse(availableFormats.get(0));
    }

    public static int chooseSwapPresentMode(IntBuffer availablePresentModes) {

        for(int i = 0;i < availablePresentModes.capacity();i++) {
            if(availablePresentModes.get(i) == VK_PRESENT_MODE_MAILBOX_KHR) {
                return availablePresentModes.get(i);
            }
        }

        return VK_PRESENT_MODE_FIFO_KHR;
    }

    public static VkExtent2D chooseSwapExtent(VkSurfaceCapabilitiesKHR capabilities, long window) {

        if(capabilities.currentExtent().width() != UINT32_MAX) {
            return capabilities.currentExtent();
        }

        IntBuffer width = stackGet().ints(0);
        IntBuffer height = stackGet().ints(0);

        glfwGetFramebufferSize(window, width, height);

        VkExtent2D actualExtent = VkExtent2D.malloc().set(width.get(0), height.get(0));

        VkExtent2D minExtent = capabilities.minImageExtent();
        VkExtent2D maxExtent = capabilities.maxImageExtent();

        actualExtent.width(clamp(minExtent.width(), maxExtent.width(), actualExtent.width()));
        actualExtent.height(clamp(minExtent.height(), maxExtent.height(), actualExtent.height()));

        return actualExtent;
    }

    public static int clamp(int min, int max, int value) {
        return Math.max(min, Math.min(max, value));
    }

    public static boolean isDeviceSuitable(VkPhysicalDevice device, long surface, Set<String> deviceExtensions) {

        QueueFamilyIndices indices = findQueueFamilies(device, surface);

        boolean extensionsSupported = checkDeviceExtensionSupport(device, deviceExtensions);
        boolean swapChainAdequate = false;
        boolean anisotropySupported = false;

        if(extensionsSupported) {
            try(MemoryStack stack = stackPush()) {
                SwapChainSupportDetails swapChainSupport = querySwapChainSupport(device, surface, stack);
                swapChainAdequate = swapChainSupport.formats.hasRemaining() && swapChainSupport.presentModes.hasRemaining();
                VkPhysicalDeviceFeatures supportedFeatures = VkPhysicalDeviceFeatures.malloc(stack);
                vkGetPhysicalDeviceFeatures(device, supportedFeatures);
                anisotropySupported = supportedFeatures.samplerAnisotropy();
            }
        }

        return indices.isComplete() && extensionsSupported && swapChainAdequate && anisotropySupported;
    }

    public static AllocatedBuffer createBufferVMA(long vmaAllocator, long size, int bufferUsageFlags, int vmaMemoryUsage, Integer vmaFlags) {
        try (var stack = stackPush()) {
            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack);
            bufferInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
            bufferInfo.size(size);
            bufferInfo.usage(bufferUsageFlags);
            bufferInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            var vmaAllocationCreateInfo = VmaAllocationCreateInfo.calloc(stack);
            vmaAllocationCreateInfo.usage(vmaMemoryUsage);
            if(vmaFlags != null) {
                vmaAllocationCreateInfo.flags(vmaFlags.intValue());
            }

            var newBuffer = new AllocatedBuffer();
            var pBuffer = stack.mallocLong(1);
            var pAllocation = stack.callocPointer(1);

            if(vmaCreateBuffer(vmaAllocator, bufferInfo, vmaAllocationCreateInfo, pBuffer, pAllocation, null) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create vma buffer");
            }

            newBuffer.buffer = pBuffer.get(0);
            newBuffer.allocation = pAllocation.get(0);

            return newBuffer;
        }
    }

    public static long createAllocator(VkPhysicalDevice physicalDevice, VkDevice device, VkInstance instance) {
        try (var stack = stackPush()) {

            VmaVulkanFunctions vmaVulkanFunctions = VmaVulkanFunctions.calloc(stack)
                    .set(instance, device);

            VmaAllocatorCreateInfo allocatorInfo = VmaAllocatorCreateInfo.calloc();
            allocatorInfo.vulkanApiVersion(VK_API_VERSION_1_3);

            allocatorInfo
                    .physicalDevice(physicalDevice)
                    .device(device)
                    .instance(instance)
                    .pVulkanFunctions(vmaVulkanFunctions);

            PointerBuffer allocator = stack.mallocPointer(1);
            if(vmaCreateAllocator(allocatorInfo, allocator) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create VMA allocator");
            }

            var allocatorVal = allocator.get(0);
            deletionQueue.add(() -> vmaDestroyAllocator(allocatorVal));
            return allocatorVal;
        }
    }

    public static VkImageCreateInfo createImageCreateInfo(int format, int usageFlags, VkExtent3D extent, Integer mipLevels, int tiling, int numLayers, Integer numSamples, MemoryStack stack) {

        VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack);
        imageInfo.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
        imageInfo.imageType(VK_IMAGE_TYPE_2D);
        imageInfo.extent(extent);

        imageInfo.mipLevels(mipLevels);
        imageInfo.arrayLayers(numLayers);
        imageInfo.format(format);
        imageInfo.tiling(tiling);
        imageInfo.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
        imageInfo.usage(usageFlags);

        imageInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
        imageInfo.samples(numSamples);

        return imageInfo;
    }

    public static VkImageViewCreateInfo createImageViewCreateInfo(int format, long image, int aspectFlags,
                                                                  int mipLevels, int layerCount,
                                                                  int imageViewType, MemoryStack stack) {
        VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack);
        viewInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
        viewInfo.image(image);
        viewInfo.viewType(imageViewType);
        viewInfo.format(format);

        if (format >= VK_FORMAT_D16_UNORM_S8_UINT) {
            aspectFlags = aspectFlags | VK_IMAGE_ASPECT_STENCIL_BIT;
        }
        viewInfo.subresourceRange().aspectMask(aspectFlags);

        viewInfo.subresourceRange().baseMipLevel(0);
        viewInfo.subresourceRange().levelCount(mipLevels);
        viewInfo.subresourceRange().baseArrayLayer(0);
        viewInfo.subresourceRange().layerCount(layerCount);

        return viewInfo;
    }

    public static VkCommandBufferBeginInfo createCommandBufferBeginInfo(int flags) {
        try (var stack = stackPush()) {
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            beginInfo.flags(flags);
            return beginInfo;
        }
    }

    public static VkCommandBufferAllocateInfo createCommandBufferAllocateInfo(long commandPool, int count, int commandBufferLevel, MemoryStack stack) {
        VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
        allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
        allocInfo.level(commandBufferLevel);
        allocInfo.commandPool(commandPool);
        allocInfo.commandBufferCount(count);

        return allocInfo;
    }

    public static long createCommandPool(VkDevice device, VkCommandPoolCreateInfo createInfo, MemoryStack stack) {

        var pCommandPool = stack.callocLong(1);
        if(vkCreateCommandPool(device, createInfo, null, pCommandPool) != VK_SUCCESS) {
            throw new RuntimeException("Error occured while creating command pool");
        }
        return pCommandPool.get(0);
    }

    public static long createFence(VkFenceCreateInfo fenceInfo) {
        try (var stack = stackPush()) {
            LongBuffer pFence = stack.mallocLong(1);

            if(vkCreateFence(device, fenceInfo, null, pFence) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create VkFence for uploadContext");
            }
           return pFence.get(0);
        }
    }

    public static VkCommandBuffer createCommandBuffer(VkDevice device, VkCommandBufferAllocateInfo allocInfo, MemoryStack stack) {
        var pCommandBuffer = stack.callocPointer(1);
        if (vkAllocateCommandBuffers(device, allocInfo, pCommandBuffer) != VK_SUCCESS) {
            throw new RuntimeException("Error occured while creating command buffer");
        }
        return new VkCommandBuffer(pCommandBuffer.get(0), device);
    }

    public static VkCommandPoolCreateInfo createCommandPoolCreateInfo(int queueFamilyIndex, int flags, MemoryStack stack) {
        log("queue family index queue;: "+ queueFamilyIndex);
        VkCommandPoolCreateInfo info = VkCommandPoolCreateInfo.calloc(stack);
        info.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
        info.queueFamilyIndex(queueFamilyIndex);
        info.flags(flags);
        return info;
    }

    public static VkSubmitInfo.Buffer createSubmitInfo(VkCommandBuffer cmd, MemoryStack stack) {
        var submitInfo = VkSubmitInfo.calloc(1, stack);
        submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
        submitInfo.waitSemaphoreCount(0);
        submitInfo.pWaitSemaphores(null);
        submitInfo.pWaitDstStageMask(null);
        submitInfo.pCommandBuffers(stack.pointers(cmd));
        return submitInfo;
    }

    public static void submitImmediateCommand(Consumer<VkCommandBuffer> func, SingleTimeCommandContext singleTimeCommandContext, VkQueue queue) {
        try (var stack = stackPush()) {
            var cmdBeginInfo = createCommandBufferBeginInfo(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
            vkCheck(vkBeginCommandBuffer(singleTimeCommandContext.commandBuffer, cmdBeginInfo));

            func.accept(singleTimeCommandContext.commandBuffer);

            vkCheck(vkEndCommandBuffer(singleTimeCommandContext.commandBuffer));

            var submit = createSubmitInfo(singleTimeCommandContext.commandBuffer, stack);
            vkCheck(vkQueueSubmit(queue, submit, singleTimeCommandContext.fence));

            vkWaitForFences(device, singleTimeCommandContext.fence, true, 999999999);
            vkResetFences(device, singleTimeCommandContext.fence);

            vkResetCommandPool(device, singleTimeCommandContext.commandPool, 0);
        }
    }

    public static long createImageView(VkImageViewCreateInfo viewInfo, VkDevice device) {
        try (var stack = stackPush()) {
            LongBuffer pImageView = stack.mallocLong(1);
            vkCheck(vkCreateImageView(device, viewInfo, null, pImageView));
            return pImageView.get(0);
        }
    }

    public static VkPhysicalDeviceProperties getGPUProperties(VkPhysicalDevice physicalDevice) {
        try (var stack = stackPush()) {
            var physicalDeviceProperties = VkPhysicalDeviceProperties.calloc(stack);
            vkGetPhysicalDeviceProperties(physicalDevice, physicalDeviceProperties);
            return physicalDeviceProperties;
        }
    }

    public static long getMinBufferOffsetAlignment(VkPhysicalDeviceProperties gpuProperties) {
        return gpuProperties.limits().minUniformBufferOffsetAlignment();
    }

    public static int getMaxUsableSampleCount(VkPhysicalDeviceProperties physicalDeviceProperties) {
        try (var stack = stackPush()) {
            int sampleCountFlags = physicalDeviceProperties.limits().framebufferColorSampleCounts()
                    & physicalDeviceProperties.limits().framebufferDepthSampleCounts();

            if((sampleCountFlags & VK_SAMPLE_COUNT_64_BIT) != 0) {
                return VK_SAMPLE_COUNT_64_BIT;
            }
            if((sampleCountFlags & VK_SAMPLE_COUNT_32_BIT) != 0) {
                return VK_SAMPLE_COUNT_32_BIT;
            }
            if((sampleCountFlags & VK_SAMPLE_COUNT_16_BIT) != 0) {
                return VK_SAMPLE_COUNT_16_BIT;
            }
            if((sampleCountFlags & VK_SAMPLE_COUNT_8_BIT) != 0) {
                return VK_SAMPLE_COUNT_8_BIT;
            }
            if((sampleCountFlags & VK_SAMPLE_COUNT_4_BIT) != 0) {
                return VK_SAMPLE_COUNT_4_BIT;
            }
            if((sampleCountFlags & VK_SAMPLE_COUNT_2_BIT) != 0) {
                return VK_SAMPLE_COUNT_2_BIT;
            }

            return VK_SAMPLE_COUNT_1_BIT;
        }
    }

    public static int findDepthFormat() {
        return findSupportedFormat(
                physicalDevice,
                stackGet().ints(VK_FORMAT_D32_SFLOAT, VK_FORMAT_D32_SFLOAT_S8_UINT, VK_FORMAT_D24_UNORM_S8_UINT),
                VK_IMAGE_TILING_OPTIMAL,
                VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT);
    }

    public static int findSupportedFormat(VkPhysicalDevice physicalDevice, IntBuffer formatCandidates, int tiling, int features) {

        try(MemoryStack stack = stackPush()) {

            VkFormatProperties props = VkFormatProperties.calloc(stack);

            for(int i = 0; i < formatCandidates.capacity(); ++i) {

                int format = formatCandidates.get(i);

                vkGetPhysicalDeviceFormatProperties(physicalDevice, format, props);

                if(tiling == VK_IMAGE_TILING_LINEAR && (props.linearTilingFeatures() & features) == features) {
                    return format;
                } else if(tiling == VK_IMAGE_TILING_OPTIMAL && (props.optimalTilingFeatures() & features) == features) {
                    return format;
                }

            }
        }

        throw new RuntimeException("Failed to find supported format");
    }

    public static boolean hasStencilComponent(long format) {
        return format == VK_FORMAT_D32_SFLOAT_S8_UINT || format == VK_FORMAT_D24_UNORM_S8_UINT;
    }


    public static void memcpyInt(ByteBuffer buffer, List<Integer> indices) {
        for(int index : indices) {
            buffer.putInt(index);
        }
        buffer.rewind();
    }

    public static void memcpy(ByteBuffer dst, ByteBuffer src, long size) {
        src.limit((int)size);
        dst.put(src);
        src.limit(src.capacity()).rewind();
    }

    public static void memcpy(ByteBuffer buffer, Mesh mesh) {
        for(int i = 0; i < mesh.getVertices().size(); i++) {
            buffer.putFloat(mesh.getVertices().get(i).get(0));
            buffer.putFloat(mesh.getVertices().get(i).get(1));
            buffer.putFloat(mesh.getVertices().get(i).get(2));

            buffer.putFloat(mesh.getAttributeList(Mesh.TEXTURE).get(i).get(0));
            buffer.putFloat(mesh.getAttributeList(Mesh.TEXTURE).get(i).get(1));

            buffer.putFloat(mesh.getAttributeList(Mesh.NORMAL).get(i).get(0));
            buffer.putFloat(mesh.getAttributeList(Mesh.NORMAL).get(i).get(1));
            buffer.putFloat(mesh.getAttributeList(Mesh.NORMAL).get(i).get(2));
        }
    }

    public static long padUniformBufferSize(long originalSize, long minUniformBufferOffsetAlignment) {
        long alignedSize = originalSize;
        if(minUniformBufferOffsetAlignment > 0) {
            alignedSize = (alignedSize + minUniformBufferOffsetAlignment - 1) & ~(minUniformBufferOffsetAlignment - 1);
        }
        return alignedSize;
    }

    public static VkDescriptorSetLayoutBinding createDescriptorSetLayoutBinding(int binding, int descriptorType, int stageFlags, MemoryStack stack) {
        VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(1);

        VkDescriptorSetLayoutBinding bufferBinding = bindings.get(0);
        bufferBinding.binding(binding);
        bufferBinding.descriptorCount(1);
        bufferBinding.descriptorType(descriptorType);
        bufferBinding.stageFlags(stageFlags);

        return bufferBinding;
    }

    public static long createDescriptorSetLayout(VkDescriptorSetLayoutBinding[] bindingsList, MemoryStack stack) {

        VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(bindingsList.length, stack);
        for(int i = 0; i < bindingsList.length; i++) {
            bindings.put(i, bindingsList[i]);
        }

        VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack);
        layoutInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
        layoutInfo.pBindings(bindings);

        LongBuffer pDescriptorSetLayout = stack.mallocLong(1);

        if(vkCreateDescriptorSetLayout(device, layoutInfo, null, pDescriptorSetLayout) != VK_SUCCESS) {
            throw new RuntimeException("Failed to create descriptor set layout");
        }

        return pDescriptorSetLayout.get(0);
    }

    public static VkWriteDescriptorSet createWriteDescriptorSet(int type, Long dstSet, VkDescriptorBufferInfo.Buffer bufferInfo, int binding, MemoryStack stack) {
        VkWriteDescriptorSet.Buffer writes = VkWriteDescriptorSet.calloc(1, stack);
        var write = writes.get(0);

        write.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);

        // descriptor set binding number
        write.dstBinding(binding);

        // Set the descriptor set
        write.dstSet(dstSet);

        write.descriptorCount(1);
        write.descriptorType(type);
        write.pBufferInfo(bufferInfo);

        return write;
    }

    public static VkWriteDescriptorSet createWriteDescriptorSet(int type, long dstSet, VkDescriptorImageInfo.Buffer imageInfo, int binding, MemoryStack stack) {
        VkWriteDescriptorSet.Buffer writes = VkWriteDescriptorSet.calloc(1, stack);
        var write = writes.get(0);

        write.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);

        // descriptor set binding number
        write.dstBinding(binding);

        // Set the descriptor set
        write.dstSet(dstSet);

        write.descriptorCount(1);
        write.descriptorType(type);
        write.pImageInfo(imageInfo);

        return write;
    }

    public static VkPhysicalDevice pickPhysicalDevice(VkInstance instance, long surface, Set<String> deviceExtensions) {

        try(MemoryStack stack = stackPush()) {

            IntBuffer deviceCount = stack.ints(0);

            vkEnumeratePhysicalDevices(instance, deviceCount, null);

            if(deviceCount.get(0) == 0) {
                throw new RuntimeException("Failed to find GPUs with Vulkan support");
            }

            PointerBuffer ppPhysicalDevices = stack.mallocPointer(deviceCount.get(0));

            vkEnumeratePhysicalDevices(instance, deviceCount, ppPhysicalDevices);

            for(int i = 0;i < ppPhysicalDevices.capacity();i++) {
                var device = new VkPhysicalDevice(ppPhysicalDevices.get(i), instance);

                if(isDeviceSuitable(device, surface, deviceExtensions)) {
                   return device;
                }
            }

            throw new RuntimeException("Failed to find a suitable GPU");
        }
    }

    public static int findMemoryType(int typeFilter, int properties, VkPhysicalDevice physicalDevice) {

        VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.malloc();
        vkGetPhysicalDeviceMemoryProperties(physicalDevice, memProperties);

        for(int i = 0;i < memProperties.memoryTypeCount();i++) {
            if((typeFilter & (1 << i)) != 0 && (memProperties.memoryTypes(i).propertyFlags() & properties) == properties) {
                return i;
            }
        }

        throw new RuntimeException("Failed to find suitable memory type");
    }

    public static boolean checkDeviceExtensionSupport(VkPhysicalDevice device, Set<String> deviceExtensions) {

        try(MemoryStack stack = stackPush()) {

            IntBuffer extensionCount = stack.ints(0);

            vkEnumerateDeviceExtensionProperties(device, (String)null, extensionCount, null);

            VkExtensionProperties.Buffer availableExtensions = VkExtensionProperties.malloc(extensionCount.get(0), stack);

            vkEnumerateDeviceExtensionProperties(device, (String)null, extensionCount, availableExtensions);

            return availableExtensions.stream()
                    .map(VkExtensionProperties::extensionNameString)
                    .collect(toSet())
                    .containsAll(deviceExtensions);
        }
    }

    public record SwapChainSupportDetails (VkSurfaceCapabilitiesKHR capabilities, VkSurfaceFormatKHR.Buffer formats, IntBuffer presentModes) { }

    public static SwapChainSupportDetails querySwapChainSupport(VkPhysicalDevice device, long surface, MemoryStack stack) {

        var capabilities = VkSurfaceCapabilitiesKHR.malloc(stack);
        VkSurfaceFormatKHR.Buffer formats = null;
        IntBuffer presentModes = null;

        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, surface, capabilities);

        IntBuffer count = stack.ints(0);

        vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, count, null);

        if(count.get(0) != 0) {
            formats = VkSurfaceFormatKHR.malloc(count.get(0), stack);
            vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, count, formats);
        }

        vkGetPhysicalDeviceSurfacePresentModesKHR(device,surface, count, null);

        if(count.get(0) != 0) {
            presentModes = stack.mallocInt(count.get(0));
            vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, count, presentModes);
        }

        return new SwapChainSupportDetails(capabilities, formats, presentModes);
    }

    public static long createSurface(VkInstance instance, long window) {

        try(MemoryStack stack = stackPush()) {

            LongBuffer pSurface = stack.longs(VK_NULL_HANDLE);

            if(glfwCreateWindowSurface(instance, window, null, pSurface) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create window surface");
            }

            var surface = pSurface.get(0);
            deletionQueue.add(() -> vkDestroySurfaceKHR(instance, surface, null));

            return surface;
        }
    }

    public static QueueFamilyIndices findQueueFamilies(VkPhysicalDevice device, long surface) {

        QueueFamilyIndices indices = new QueueFamilyIndices();

        try(MemoryStack stack = stackPush()) {

            IntBuffer queueFamilyCount = stack.ints(0);

            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, null);

            VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.malloc(queueFamilyCount.get(0), stack);

            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, queueFamilies);

            IntBuffer presentSupport = stack.ints(VK_FALSE);

            for(int i = 0;i < queueFamilies.capacity() || !indices.isComplete();i++) {

                if((queueFamilies.get(i).queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) {
                    indices.graphicsFamily = i;
                }

                vkGetPhysicalDeviceSurfaceSupportKHR(device, i, surface, presentSupport);

                if(presentSupport.get(0) == VK_TRUE) {
                    indices.presentFamily = i;
                }
            }

            return indices;
        }
    }

    public static PointerBuffer asPointerBuffer(Collection<String> collection) {

        MemoryStack stack = stackGet();

        PointerBuffer buffer = stack.mallocPointer(collection.size());

        collection.stream()
                .map(stack::UTF8)
                .forEach(buffer::put);

        return buffer.rewind();
    }

    public static PointerBuffer asPointerBuffer(List<? extends Pointer> list) {

        MemoryStack stack = stackGet();

        PointerBuffer buffer = stack.mallocPointer(list.size());

        list.forEach(buffer::put);

        return buffer.rewind();
    }

    public static PointerBuffer getRequiredExtensions(boolean areValidationLayersEnabled) {

        PointerBuffer glfwExtensions = glfwGetRequiredInstanceExtensions();

        if(areValidationLayersEnabled) {

            MemoryStack stack = stackGet();

            PointerBuffer extensions = stack.mallocPointer(glfwExtensions.capacity() + 1);

            extensions.put(glfwExtensions);
            extensions.put(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME));

            // Rewind the buffer before returning it to reset its position back to 0
            return extensions.rewind();
        }

        return glfwExtensions;
    }

    public static boolean checkValidationLayerSupport(Set<String> validationLayers) {

        if(validationLayers == null) {
            return true;
        }

        try(MemoryStack stack = stackPush()) {

            IntBuffer layerCount = stack.ints(0);

            vkEnumerateInstanceLayerProperties(layerCount, null);

            VkLayerProperties.Buffer availableLayers = VkLayerProperties.malloc(layerCount.get(0), stack);

            vkEnumerateInstanceLayerProperties(layerCount, availableLayers);

            Set<String> availableLayerNames = availableLayers.stream()
                    .map(VkLayerProperties::layerNameString)
                    .collect(toSet());

            return availableLayerNames.containsAll(validationLayers);
        }
    }

    public static void vkCheck(int success) {
        if(success != VK_SUCCESS) {
            throw new RuntimeException("VK: Error occured");
        }
    }

}
