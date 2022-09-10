package main;

import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.LongBuffer;

import static Kurama.Vulkan.VulkanUtilities.device;
import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.vulkan.VK10.*;

public class ViewRenderPassFrame {

    public long presentCompleteSemaphore;
    public long renderFinishedSemaphore;
    public long fence;
    public long commandPool;
    public VkCommandBuffer commandBuffer;

    public long imageInputDescriptorSet;

    public void cleanUp() {
        vkDestroySemaphore(device, renderFinishedSemaphore(), null);
        vkDestroySemaphore(device, presentCompleteSemaphore(), null);
        vkDestroyFence(device, fence(), null);
        vkDestroyCommandPool(device, commandPool, null);
    }

    public long presentCompleteSemaphore() {
        return presentCompleteSemaphore;
    }

    public LongBuffer pPresentCompleteSemaphore() {
        return stackGet().longs(presentCompleteSemaphore);
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
