package Kurama.Vulkan;

import main.GameVulkan;
import main.RenderingEngineVulkan;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;

import java.nio.LongBuffer;

import static Kurama.Vulkan.Vulkan.device;
import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.util.vma.Vma.vmaDestroyBuffer;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Wraps the needed sync objects for an in flight frame
 *
 * This frame's sync objects must be deleted manually
 * */
public class Frame {

    public long imageAvailableSemaphore;
    public long renderFinishedSemaphore;
    public long fence;
    public long commandPool;
    public VkCommandBuffer commandBuffer;
    public AllocatedBuffer cameraBuffer;
    public AllocatedBuffer objectBuffer;

    // Global Descriptor set contains the camera data and other scene parameters
    public long globalDescriptorSet;

    // This contains the object transformation matrices
    public long objectDescriptorSet;

    public RenderingEngineVulkan renderingEngine;

    public Frame(RenderingEngineVulkan renderingEngine, long imageAvailableSemaphore, long renderFinishedSemaphore, long fence, long commandPool, VkCommandBuffer commandBuffer) {
        this.imageAvailableSemaphore = imageAvailableSemaphore;
        this.renderFinishedSemaphore = renderFinishedSemaphore;
        this.fence = fence;
        this.commandBuffer = commandBuffer;
        this.commandPool = commandPool;
        this.renderingEngine = renderingEngine;
    }

    public Frame(RenderingEngineVulkan renderingEngine) {
        this.renderingEngine = renderingEngine;
    }

    public void cleanUp() {
        vkDestroySemaphore(device, renderFinishedSemaphore(), null);
        vkDestroySemaphore(device, presentSemaphore(), null);
        vkDestroyFence(device, fence(), null);
        vkDestroyCommandPool(device, commandPool, null);
        vmaDestroyBuffer(renderingEngine.vmaAllocator, cameraBuffer.buffer, cameraBuffer.allocation);
        vmaDestroyBuffer(renderingEngine.vmaAllocator, objectBuffer.buffer, objectBuffer.allocation);
    }

    public long presentSemaphore() {
        return imageAvailableSemaphore;
    }

    public LongBuffer pImageAvailableSemaphore() {
        return stackGet().longs(imageAvailableSemaphore);
    }

    public long renderFinishedSemaphore() {
        return renderFinishedSemaphore;
    }

    public LongBuffer pRenderFinishedSemaphore() {
        return stackGet().longs(renderFinishedSemaphore);
    }

    public long fence() {
        return fence;
    }

    public LongBuffer pFence() {
        return stackGet().longs(fence);
    }
}
