package main;

import Kurama.Vulkan.Frame;
import Kurama.Vulkan.ShaderSPIRVUtils;
import Kurama.Vulkan.Vulkan;
import Kurama.display.DisplayVulkan;
import Kurama.game.Game;
import Kurama.utils.Logger;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.stream.Stream;

import static Kurama.Vulkan.ShaderSPIRVUtils.ShaderKind.FRAGMENT_SHADER;
import static Kurama.Vulkan.ShaderSPIRVUtils.ShaderKind.VERTEX_SHADER;
import static Kurama.Vulkan.ShaderSPIRVUtils.compileShaderFile;
import static Kurama.Vulkan.Vulkan.*;
import static Kurama.utils.Logger.log;
import static java.lang.ClassLoader.getSystemClassLoader;
import static java.util.stream.Collectors.toSet;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.Configuration.DEBUG;
import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.vkDestroyInstance;
import static org.lwjgl.vulkan.VK13.VK_API_VERSION_1_3;

public class GameVulkan extends Game {

    private static final int MAX_FRAMES_IN_FLIGHT = 2;

    private static final boolean ENABLE_VALIDATION_LAYERS = DEBUG.get(true);

    private static final Set<String> VALIDATION_LAYERS;
    static {
        if(ENABLE_VALIDATION_LAYERS) {
            VALIDATION_LAYERS = new HashSet<>();
            VALIDATION_LAYERS.add("VK_LAYER_KHRONOS_validation");
        } else {
            // We are not going to use it, so we don't create it
            VALIDATION_LAYERS = null;
        }
    }

    private static final Set<String> DEVICE_EXTENSIONS = Stream.of(VK_KHR_SWAPCHAIN_EXTENSION_NAME)
            .collect(toSet());

    private VkInstance instance;
    private long debugMessenger;
    private long surface;
    private VkPhysicalDevice physicalDevice;
    private VkDevice device;
    private long vertexBuffer;
    private long vertexBufferMemory;
    private long indexBuffer;
    private long indexBufferMemory;
    private long textureImage;
    private long textureImageMemory;

    private long textureSampler;
    private VkQueue graphicsQueue;
    private VkQueue presentQueue;
    private long swapChain;
    private List<Long> swapChainImages;
    private int swapChainImageFormat;
    private VkExtent2D swapChainExtent;
    private List<Long> swapChainImageViews;
    private long textureImageView;

    long depthImage;
    long depthImageMemory;
    long depthImageView;
    private List<Long> swapChainFramebuffers;
    private long renderPass;
    private long descriptorPool;
    private long descriptorSetLayout;
    private List<Long> descriptorSets;
    private long pipelineLayout;
    private long graphicsPipeline;
    private long commandPool;
    private List<VkCommandBuffer> commandBuffers;

    private List<Frame> inFlightFrames;
    private Map<Integer, Frame> imagesInFlight;
    private int currentFrame;
    boolean framebufferResize;
    private List<Long> uniformBuffers;
    private List<Long> uniformBuffersMemory;

    public DisplayVulkan display;
    public UniformBufferObject currentUbo;

    private static final Vertex[] VERTICES = {
            new Vertex(new Vector3f(-0.5f, -0.5f, 0f), new Vector3f(1.0f, 0.0f, 0.0f), new Vector2f(1.0f, 0.0f)),
            new Vertex(new Vector3f(0.5f, -0.5f, 0f), new Vector3f(0.0f, 1.0f, 0.0f), new Vector2f(0.0f, 0.0f)),
            new Vertex(new Vector3f(0.5f, 0.5f, 0f), new Vector3f(0.0f, 0.0f, 1.0f), new Vector2f(0.0f, 1.0f)),
            new Vertex(new Vector3f(-0.5f, 0.5f, 0f), new Vector3f(1.0f, 1.0f, 1.0f), new Vector2f(1.0f, 1.0f)),

            new Vertex(new Vector3f(-0.5f, -0.5f, -0.5f), new Vector3f(1.0f, 0.0f, 0.0f), new Vector2f(0.0f, 0.0f)),
            new Vertex(new Vector3f(0.5f, -0.5f, -0.5f ), new Vector3f(0.0f, 1.0f, 0.0f), new Vector2f(1.0f, 0.0f)),
            new Vertex(new Vector3f(0.5f, 0.5f, -0.5f  ), new Vector3f(0.0f, 0.0f, 1.0f), new Vector2f(1.0f, 1.0f)),
            new Vertex(new Vector3f(-0.5f, 0.5f, -0.5f ), new Vector3f(1.0f, 1.0f, 1.0f), new Vector2f(0.0f, 1.0f))
    };

    private static final /*uint16_t*/ short[] INDICES = {
            0, 1, 2, 2, 3, 0,
            4, 5, 6, 6, 7, 4
    };

    public GameVulkan(String threadName) {
        super(threadName);
    }

    @Override
    public void init() {
        display = new DisplayVulkan(this);
        display.resizeEvents.add(() -> {this.framebufferResize = true;});
        initVulkan();
        this.setTargetFPS(Integer.MAX_VALUE);
        this.shouldDisplayFPS = true;

        rotateRectangle();
    }

    @Override
    public void tick() {
        glfwPollEvents();

        if(glfwWindowShouldClose(display.window)) {
            programRunning = false;
        }
    }

    @Override
    public void render() {
        drawFrame();
    }

    private void drawFrame() {

        try(MemoryStack stack = stackPush()) {

            Frame thisFrame = inFlightFrames.get(currentFrame);

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

            updateUbo(imageIndex);

            if(imagesInFlight.containsKey(imageIndex)) {
                vkWaitForFences(device, imagesInFlight.get(imageIndex).fence(), true, UINT64_MAX);
            }

            imagesInFlight.put(imageIndex, thisFrame);

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);

            submitInfo.waitSemaphoreCount(1);
            submitInfo.pWaitSemaphores(thisFrame.pImageAvailableSemaphore());
            submitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));

            submitInfo.pSignalSemaphores(thisFrame.pRenderFinishedSemaphore());

            submitInfo.pCommandBuffers(stack.pointers(commandBuffers.get(imageIndex)));

            vkResetFences(device, thisFrame.pFence());

            if((vkResult = vkQueueSubmit(graphicsQueue, submitInfo, thisFrame.fence())) != VK_SUCCESS) {
                vkResetFences(device, thisFrame.pFence());
                throw new RuntimeException("Failed to submit draw command buffer: " + vkResult);
            }

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

    @Override
    public void cleanUp() {
        // Wait for the device to complete all operations before release resources
        display.cleanUp();
        vkDeviceWaitIdle(device);

        cleanupSwapChain();

        vkDestroyImageView(device, textureImageView, null);
        vkDestroySampler(device, textureSampler, null);
        vkDestroyImage(device, textureImage, null);
        vkFreeMemory(device, textureImageMemory, null);

        vkDestroyDescriptorPool(device, descriptorPool, null);
        vkDestroyDescriptorSetLayout(device, descriptorSetLayout, null);

        vkDestroyBuffer(device, vertexBuffer, null);
        vkFreeMemory(device, vertexBufferMemory, null);

        vkDestroyBuffer(device, indexBuffer, null);
        vkFreeMemory(device, indexBufferMemory, null);

        uniformBuffers.forEach(ubo -> vkDestroyBuffer(device, ubo, null));
        uniformBuffersMemory.forEach(uboMemory -> vkFreeMemory(device, uboMemory, null));

        inFlightFrames.forEach(frame -> {
            vkDestroySemaphore(device, frame.renderFinishedSemaphore(), null);
            vkDestroySemaphore(device, frame.imageAvailableSemaphore(), null);
            vkDestroyFence(device, frame.fence(), null);
        });
        inFlightFrames.clear();

        vkDestroyCommandPool(device, commandPool, null);

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
        createInstance();
        setupDebugMessenger();

        surface = createSurface(instance, display.window);
        physicalDevice = pickPhysicalDevice(instance, surface, DEVICE_EXTENSIONS);

        createLogicalDevice();

        createCommandPool();

        createTextureImage();
        createTextureImageView();
        createTextureSampler();

        createVertexBuffer();
        createIndexBuffer();
        createDescriptorSetLayout();

        createSwapChain();

        createImageViews();
        createRenderPass();
        createGraphicsPipeline();
        createDepthResources();
        createFramebuffers();
        createUniformBuffers();
        createDescriptorPool();
        createDescriptorSets();
        createCommandBuffers();

        createSyncObjects();
    }

    private void createInstance() {

        if(ENABLE_VALIDATION_LAYERS && !checkValidationLayerSupport(VALIDATION_LAYERS)) {
            throw new RuntimeException("Validation requested but not supported");
        }

        try(MemoryStack stack = stackPush()) {

            // Use calloc to initialize the structs with 0s. Otherwise, the program can crash due to random values
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack);

            appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO);
            appInfo.pApplicationName(stack.UTF8Safe("Hello Triangle"));
            appInfo.applicationVersion(VK_MAKE_VERSION(1, 0, 0));
            appInfo.pEngineName(stack.UTF8Safe("No Engine"));
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
                populateDebugMessengerCreateInfo(debugCreateInfo);
                createInfo.pNext(debugCreateInfo.address());
            }

            // We need to retrieve the pointer of the created instance
            PointerBuffer instancePtr = stack.mallocPointer(1);

            if(vkCreateInstance(createInfo, null, instancePtr) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create instance");
            }

            instance = new VkInstance(instancePtr.get(0), createInfo);
        }
    }
    private void populateDebugMessengerCreateInfo(VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo) {
        debugCreateInfo.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT);
        debugCreateInfo.messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT);
        debugCreateInfo.messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT);
        debugCreateInfo.pfnUserCallback(GameVulkan::debugCallback);
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
        createDepthResources();
        createFramebuffers();
        createCommandBuffers();
    }

    private void cleanupSwapChain() {

        swapChainFramebuffers.forEach(framebuffer -> vkDestroyFramebuffer(device, framebuffer, null));

        vkFreeCommandBuffers(device, commandPool, Vulkan.asPointerBuffer(commandBuffers));

        vkDestroyPipeline(device, graphicsPipeline, null);

        vkDestroyPipelineLayout(device, pipelineLayout, null);

        vkDestroyRenderPass(device, renderPass, null);

        swapChainImageViews.forEach(imageView -> vkDestroyImageView(device, imageView, null));

        vkDestroyImageView(device, depthImageView, null);
        vkDestroyImage(device, depthImage, null);
        vkFreeMemory(device, depthImageMemory, null);

        vkDestroySwapchainKHR(device, swapChain, null);
    }

    private void createDepthResources() {
        try(MemoryStack stack = stackPush()) {

            int depthFormat = findDepthFormat();

            LongBuffer pDepthImage = stack.mallocLong(1);
            LongBuffer pDepthImageMemory = stack.mallocLong(1);

            createImage(swapChainExtent.width(), swapChainExtent.height(), depthFormat, VK_IMAGE_TILING_OPTIMAL, VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, pDepthImage, pDepthImageMemory);

            depthImage = pDepthImage.get(0);
            depthImageMemory = pDepthImageMemory.get(0);

            depthImageView = createImageView(depthImage, depthFormat, VK_IMAGE_ASPECT_DEPTH_BIT, device);
            // Explicitly transitioning the depth image
            transitionImageLayout(depthImage, depthFormat,
                    VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

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
            log("swapchain format: "+ swapChainImageFormat);
            swapChainExtent = VkExtent2D.create().set(extent);
        }
    }

    private void setupDebugMessenger() {

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
        }
    }

    private static int createDebugUtilsMessengerEXT(VkInstance instance, VkDebugUtilsMessengerCreateInfoEXT createInfo,
                                                    VkAllocationCallbacks allocationCallbacks, LongBuffer pDebugMessenger) {

        if(vkGetInstanceProcAddr(instance, "vkCreateDebugUtilsMessengerEXT") != NULL) {
            return vkCreateDebugUtilsMessengerEXT(instance, createInfo, allocationCallbacks, pDebugMessenger);
        }

        return VK_ERROR_EXTENSION_NOT_PRESENT;
    }

    private void createImageViews() {
        swapChainImageViews = new ArrayList<>(swapChainImages.size());
        for(long swapChainImage : swapChainImages) {
            swapChainImageViews.add(createImageView(swapChainImage, swapChainImageFormat, VK_IMAGE_ASPECT_COLOR_BIT, device));
        }
    }

    private void createRenderPass() {

        try(MemoryStack stack = stackPush()) {

            var attachments  = VkAttachmentDescription.calloc(2, stack);
            var attachmentRefs = VkAttachmentReference.calloc(2, stack);

            // Color attachments
            var colorAttachment = attachments .get(0);
            colorAttachment.format(swapChainImageFormat);
            colorAttachment.samples(VK_SAMPLE_COUNT_1_BIT);
            colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
            colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
            colorAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            colorAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            colorAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            colorAttachment.finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

           var colorAttachmentRef = attachmentRefs.get(0);
            colorAttachmentRef.attachment(0);
            colorAttachmentRef.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            // Depth-Stencil attachments
            VkAttachmentDescription depthAttachment = attachments.get(1);
            depthAttachment.format(findDepthFormat());
            depthAttachment.samples(VK_SAMPLE_COUNT_1_BIT);
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
            multisampling.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);

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

            // ===> PIPELINE LAYOUT CREATION <===

            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack);
            pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
            pipelineLayoutInfo.pSetLayouts(stack.longs(descriptorSetLayout));

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

            LongBuffer attachments = stack.longs(VK_NULL_HANDLE, depthImageView);
            LongBuffer pFramebuffer = stack.mallocLong(1);

            // Lets allocate the create info struct once and just update the pAttachments field each iteration
            VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.calloc(stack);
            framebufferInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
            framebufferInfo.renderPass(renderPass);
            framebufferInfo.width(swapChainExtent.width());
            framebufferInfo.height(swapChainExtent.height());
            framebufferInfo.layers(1);

            for(long imageView : swapChainImageViews) {

                attachments.put(0, imageView);

                framebufferInfo.pAttachments(attachments);

                if(vkCreateFramebuffer(device, framebufferInfo, null, pFramebuffer) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create framebuffer");
                }

                swapChainFramebuffers.add(pFramebuffer.get(0));
            }
        }
    }

    private void createCommandPool() {

        try(MemoryStack stack = stackPush()) {

            QueueFamilyIndices queueFamilyIndices = findQueueFamilies(physicalDevice, surface);

            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
            poolInfo.queueFamilyIndex(queueFamilyIndices.graphicsFamily);

            LongBuffer pCommandPool = stack.mallocLong(1);

            if (vkCreateCommandPool(device, poolInfo, null, pCommandPool) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create command pool");
            }

            commandPool = pCommandPool.get(0);
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

    private void createDescriptorSetLayout() {
        try (var stack = stackPush()) {
            VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(2, stack);

            VkDescriptorSetLayoutBinding uboLayoutBinding = bindings.get(0);
            uboLayoutBinding.binding(0);
            uboLayoutBinding.descriptorCount(1);
            uboLayoutBinding.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
            uboLayoutBinding.pImmutableSamplers(null);
            uboLayoutBinding.stageFlags(VK_SHADER_STAGE_VERTEX_BIT);

            VkDescriptorSetLayoutBinding samplerLayoutBinding = bindings.get(1);
            samplerLayoutBinding.binding(1);
            samplerLayoutBinding.descriptorCount(1);
            samplerLayoutBinding.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
            samplerLayoutBinding.pImmutableSamplers(null);
            samplerLayoutBinding.stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);

            VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack);
            layoutInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
            layoutInfo.pBindings(bindings);

            LongBuffer pDescriptorSetLayout = stack.mallocLong(1);

            if(vkCreateDescriptorSetLayout(device, layoutInfo, null, pDescriptorSetLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor set layout");
            }
            descriptorSetLayout = pDescriptorSetLayout.get(0);
        }
    }

    private void createUniformBuffers() {
        try (var stack = stackPush()) {
            uniformBuffers = new ArrayList<>(swapChainImages.size());
            uniformBuffersMemory = new ArrayList<>(swapChainImages.size());

            LongBuffer pBuffer = stack.mallocLong(1);
            LongBuffer pBufferMemory = stack.mallocLong(1);

            for(int i = 0;i < swapChainImages.size(); i++) {
                createBuffer(device, physicalDevice,
                        UniformBufferObject.SIZEOF,
                        VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                        VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                        pBuffer,
                        pBufferMemory);
                uniformBuffers.add(pBuffer.get(0));
                uniformBuffersMemory.add(pBufferMemory.get(0));
            }
        }
    }

    private void createIndexBuffer() {
        try (var stack = stackPush()) {
            long bufferSize = Short.SIZE * INDICES.length;

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
                memcpy(data.getByteBuffer(0, (int) bufferSize), INDICES);
            }
            vkUnmapMemory(device, stagingBufferMemory);

            createBuffer(device, physicalDevice, bufferSize,
                    VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
                    VK_MEMORY_HEAP_DEVICE_LOCAL_BIT,
                    pBuffer,
                    pBufferMemory);

            indexBuffer = pBuffer.get(0);
            indexBufferMemory = pBufferMemory.get(0);

            copyBuffer(device, commandPool, graphicsQueue, stagingBuffer, indexBuffer, bufferSize);

            vkDestroyBuffer(device, stagingBuffer, null);
            vkFreeMemory(device, stagingBufferMemory, null);
        }
    }

    private void createVertexBuffer() {
        try (var stack = stackPush()) {
            long bufferSize = Vertex.SIZEOF * VERTICES.length;

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
                memcpy(data.getByteBuffer(0, (int) bufferSize), VERTICES);
            }
            vkUnmapMemory(device, stagingBufferMemory);

            createBuffer(device, physicalDevice, bufferSize,
                    VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                    VK_MEMORY_HEAP_DEVICE_LOCAL_BIT,
                    pBuffer,
                    pBufferMemory);

            vertexBuffer = pBuffer.get(0);
            vertexBufferMemory = pBufferMemory.get(0);

            copyBuffer(device, commandPool, graphicsQueue, stagingBuffer, vertexBuffer, bufferSize);

            vkDestroyBuffer(device, stagingBuffer, null);
            vkFreeMemory(device, stagingBufferMemory, null);
        }
    }

    private void createTextureImage() {
        try(var stack = stackPush()) {

            var filename = "C:/Users/alanj/git/Kurama-Engine/RenderEngine/projects/VulkanTestProject/code/textures/texture.jpg";

            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            IntBuffer pChannels = stack.mallocInt(1);

            ByteBuffer pixels = stbi_load(filename, pWidth, pHeight, pChannels, STBI_rgb_alpha);

            long imageSize = pWidth.get(0) * pHeight.get(0) * 4;

            if(pixels == null) {
                throw new RuntimeException("Failed to load texture image " + filename);
            }

            LongBuffer pStagingBuffer = stack.mallocLong(1);
            LongBuffer pStagingBufferMemory = stack.mallocLong(1);

            createBuffer(device, physicalDevice,
                    imageSize,
                    VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                    pStagingBuffer,
                    pStagingBufferMemory);

            PointerBuffer data = stack.mallocPointer(1);
            vkMapMemory(device, pStagingBufferMemory.get(0), 0, imageSize, 0, data);
            {
                memcpy(data.getByteBuffer(0, (int) imageSize), pixels, imageSize);
            }
            vkUnmapMemory(device, pStagingBufferMemory.get(0));

            stbi_image_free(pixels);

            LongBuffer pTextureImage = stack.mallocLong(1);
            LongBuffer pTextureImageMemory = stack.mallocLong(1);
            createImage(pWidth.get(0), pHeight.get(0),
                    VK_FORMAT_R8G8B8A8_SRGB, VK_IMAGE_TILING_OPTIMAL,
                    VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                    pTextureImage,
                    pTextureImageMemory);

            textureImage = pTextureImage.get(0);
            textureImageMemory = pTextureImageMemory.get(0);

            transitionImageLayout(textureImage,
                    VK_FORMAT_R8G8B8A8_SRGB,
                    VK_IMAGE_LAYOUT_UNDEFINED,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);

            copyBufferToImage(pStagingBuffer.get(0), textureImage, pWidth.get(0), pHeight.get(0));

            transitionImageLayout(textureImage,
                    VK_FORMAT_R8G8B8A8_SRGB,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

            vkDestroyBuffer(device, pStagingBuffer.get(0), null);
            vkFreeMemory(device, pStagingBufferMemory.get(0), null);

        }
    }

    private void createTextureImageView() {
        textureImageView = createImageView(textureImage, VK_FORMAT_R8G8B8A8_SRGB, VK_IMAGE_ASPECT_COLOR_BIT, device);
    }

    private void createTextureSampler() {
        try(MemoryStack stack = stackPush()) {

            VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.calloc(stack);
            samplerInfo.sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO);
            samplerInfo.magFilter(VK_FILTER_LINEAR);
            samplerInfo.minFilter(VK_FILTER_LINEAR);
            samplerInfo.addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT);
            samplerInfo.addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT);
            samplerInfo.addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT);
            samplerInfo.anisotropyEnable(true);
            samplerInfo.maxAnisotropy(16.0f);
            samplerInfo.borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK);
            samplerInfo.unnormalizedCoordinates(false);
            samplerInfo.compareEnable(false);
            samplerInfo.compareOp(VK_COMPARE_OP_ALWAYS);
            samplerInfo.mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR);

            LongBuffer pTextureSampler = stack.mallocLong(1);

            if(vkCreateSampler(device, samplerInfo, null, pTextureSampler) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create texture sampler");
            }

            textureSampler = pTextureSampler.get(0);
        }
    }


    private void transitionImageLayout(long image, int format, int oldLayout, int newLayout) {
        try(MemoryStack stack = stackPush()) {
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack);
            barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
            barrier.oldLayout(oldLayout);
            barrier.newLayout(newLayout);
            barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.image(image);
            barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            barrier.subresourceRange().baseMipLevel(0);
            barrier.subresourceRange().levelCount(1);
            barrier.subresourceRange().baseArrayLayer(0);
            barrier.subresourceRange().layerCount(1);

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
            }
            else {
                throw new IllegalArgumentException("Unsupported layout transition");
            }

            VkCommandBuffer commandBuffer = beginSingleTimeCommands(device, commandPool);

            vkCmdPipelineBarrier(commandBuffer,
                    sourceStage, destinationStage,
                    0,
                    null,
                    null,
                    barrier);

            endSingleTimeCommands(device, commandPool, commandBuffer, graphicsQueue);
        }
    }

    private void copyBufferToImage(long buffer, long image, int width, int height) {

        try(MemoryStack stack = stackPush()) {

            VkCommandBuffer commandBuffer = beginSingleTimeCommands(device, commandPool);

            VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack);
            region.bufferOffset(0);
            region.bufferRowLength(0);   // Tightly packed
            region.bufferImageHeight(0);  // Tightly packed
            region.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            region.imageSubresource().mipLevel(0);
            region.imageSubresource().baseArrayLayer(0);
            region.imageSubresource().layerCount(1);
            region.imageOffset().set(0, 0, 0);
            region.imageExtent(VkExtent3D.calloc(stack).set(width, height, 1));

            vkCmdCopyBufferToImage(commandBuffer, buffer, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region);

            endSingleTimeCommands(device, commandPool, commandBuffer, graphicsQueue);
        }
    }

    private void createImage(int width, int height, int format, int tiling, int usage, int memProperties,
                             LongBuffer pTextureImage, LongBuffer pTextureImageMemory) {

        try(MemoryStack stack = stackPush()) {

            VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack);
            imageInfo.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
            imageInfo.imageType(VK_IMAGE_TYPE_2D);
            imageInfo.extent().width(width);
            imageInfo.extent().height(height);
            imageInfo.extent().depth(1);
            imageInfo.mipLevels(1);
            imageInfo.arrayLayers(1);
            imageInfo.format(format);
            imageInfo.tiling(tiling);
            imageInfo.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            imageInfo.usage(usage);
            imageInfo.samples(VK_SAMPLE_COUNT_1_BIT);
            imageInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            if(vkCreateImage(device, imageInfo, null, pTextureImage) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create image");
            }

            VkMemoryRequirements memRequirements = VkMemoryRequirements.malloc(stack);
            vkGetImageMemoryRequirements(device, pTextureImage.get(0), memRequirements);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
            allocInfo.allocationSize(memRequirements.size());
            allocInfo.memoryTypeIndex(findMemoryType(memRequirements.memoryTypeBits(), memProperties, physicalDevice));

            if(vkAllocateMemory(device, allocInfo, null, pTextureImageMemory) != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate image memory");
            }

            vkBindImageMemory(device, pTextureImage.get(0), pTextureImageMemory.get(0), 0);
        }
    }

    private void createDescriptorSets() {
        try (var stack = stackPush()) {
            LongBuffer layouts = stack.mallocLong(swapChainImages.size());
            for(int i = 0;i < layouts.capacity();i++) {
                layouts.put(i, descriptorSetLayout);
            }

            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
            allocInfo.descriptorPool(descriptorPool);
            allocInfo.pSetLayouts(layouts);

            LongBuffer pDescriptorSets = stack.mallocLong(swapChainImages.size());

            if(vkAllocateDescriptorSets(device, allocInfo, pDescriptorSets) != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate descriptor sets");
            }

            descriptorSets = new ArrayList<>(pDescriptorSets.capacity());

            VkDescriptorBufferInfo.Buffer bufferInfo =VkDescriptorBufferInfo.calloc(1, stack);
            bufferInfo.offset(0);
            bufferInfo.range(UniformBufferObject.SIZEOF);

            VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack);
            imageInfo.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            imageInfo.imageView(textureImageView);
            imageInfo.sampler(textureSampler);

            VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(2, stack);

            VkWriteDescriptorSet uboDescriptorWrite  = descriptorWrites.get(0);
            uboDescriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
            uboDescriptorWrite.dstBinding(0);
            uboDescriptorWrite.dstArrayElement(0);
            uboDescriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
            uboDescriptorWrite.descriptorCount(1);
            uboDescriptorWrite.pBufferInfo(bufferInfo);

            VkWriteDescriptorSet samplerDescriptorWrite = descriptorWrites.get(1);
            samplerDescriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
            samplerDescriptorWrite.dstBinding(1);
            samplerDescriptorWrite.dstArrayElement(0);
            samplerDescriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
            samplerDescriptorWrite.descriptorCount(1);
            samplerDescriptorWrite.pImageInfo(imageInfo);

            for(int i = 0;i < pDescriptorSets.capacity();i++) {
                long descriptorSet = pDescriptorSets.get(i);

                bufferInfo.buffer(uniformBuffers.get(i));
                uboDescriptorWrite.dstSet(descriptorSet);
                samplerDescriptorWrite.dstSet(descriptorSet);

                vkUpdateDescriptorSets(device, descriptorWrites, null);
                descriptorSets.add(descriptorSet);
            }
        }
    }

    private void createDescriptorPool() {
        try (var stack = stackPush()) {

            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(2, stack);

            VkDescriptorPoolSize uniformBufferPoolSize  = poolSizes.get(0);
            uniformBufferPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
            uniformBufferPoolSize.descriptorCount(swapChainImages.size());

            VkDescriptorPoolSize textureSamplerPoolSize = poolSizes.get(1);
            textureSamplerPoolSize.type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
            textureSamplerPoolSize.descriptorCount(swapChainImages.size());


            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
            poolInfo.pPoolSizes(poolSizes);
            poolInfo.maxSets(swapChainImages.size());

            LongBuffer pDescriptorPool = stack.mallocLong(1);

            if(vkCreateDescriptorPool(device, poolInfo, null, pDescriptorPool) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor pool");
            }

            descriptorPool = pDescriptorPool.get(0);

        }
    }

    public void rotateRectangle() {
        UniformBufferObject ubo = new UniformBufferObject();

        ubo.model.rotate((float) (glfwGetTime() * Math.toRadians(90)), 0.0f, 0.0f, 1.0f);
        ubo.view.lookAt(0f, -2.0f, 2f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f);
        ubo.proj.perspective((float) Math.toRadians(45),
                (float)swapChainExtent.width() / (float)swapChainExtent.height(), 0.1f, 10.0f);
        ubo.proj.m11(ubo.proj.m11() * -1);

        this.currentUbo = ubo;
    }

    private void updateUbo(int currentImage) {
        try(MemoryStack stack = stackPush()) {

            PointerBuffer data = stack.mallocPointer(1);
            vkMapMemory(device, uniformBuffersMemory.get(currentImage), 0, UniformBufferObject.SIZEOF, 0, data);
            {
                memcpy(data.getByteBuffer(0, UniformBufferObject.SIZEOF), currentUbo);
            }
            vkUnmapMemory(device, uniformBuffersMemory.get(currentImage));
        }
    }

    private static int debugCallback(int messageSeverity, int messageType, long pCallbackData, long pUserData) {
        VkDebugUtilsMessengerCallbackDataEXT callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
        System.err.println("Validation layer: " + callbackData.pMessageString());
        return VK_FALSE;
    }

    private void createCommandBuffers() {

        final int commandBuffersCount = swapChainFramebuffers.size();

        commandBuffers = new ArrayList<>(commandBuffersCount);

        try(MemoryStack stack = stackPush()) {

            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.commandPool(commandPool);
            allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandBufferCount(commandBuffersCount);

            PointerBuffer pCommandBuffers = stack.mallocPointer(commandBuffersCount);

            if(vkAllocateCommandBuffers(device, allocInfo, pCommandBuffers) != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate command buffers");
            }

            for(int i = 0;i < commandBuffersCount;i++) {
                commandBuffers.add(new VkCommandBuffer(pCommandBuffers.get(i), device));
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

            for(int i = 0;i < commandBuffersCount;i++) {

                VkCommandBuffer commandBuffer = commandBuffers.get(i);

                if(vkBeginCommandBuffer(commandBuffer, beginInfo) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to begin recording command buffer");
                }

                renderPassInfo.framebuffer(swapChainFramebuffers.get(i));

                vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);
                {
                    vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline);

                    LongBuffer vertexBuffers = stack.longs(vertexBuffer);
                    LongBuffer offsets = stack.longs(0);
                    vkCmdBindVertexBuffers(commandBuffer, 0, vertexBuffers, offsets);
                    vkCmdBindIndexBuffer(commandBuffer, indexBuffer, 0, VK_INDEX_TYPE_UINT16);

                    vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                            pipelineLayout, 0, stack.longs(descriptorSets.get(i)), null);

                    vkCmdDrawIndexed(commandBuffer, INDICES.length, 1, 0, 0, 0);
                }
                vkCmdEndRenderPass(commandBuffer);


                if(vkEndCommandBuffer(commandBuffer) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to record command buffer");
                }

            }

        }
    }

    private void createSyncObjects() {

        inFlightFrames = new ArrayList<>(MAX_FRAMES_IN_FLIGHT);
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

                inFlightFrames.add(new Frame(pImageAvailableSemaphore.get(0), pRenderFinishedSemaphore.get(0), pFence.get(0)));
            }

        }
    }
}
