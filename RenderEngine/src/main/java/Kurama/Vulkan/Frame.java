package Kurama.Vulkan;

import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackGet;

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
//    public long cameraBuffer;
//    public long cameraBufferMemory;

    public AllocatedBuffer cameraBuffer;

    public long globalDescriptorSet;

    public Frame(long imageAvailableSemaphore, long renderFinishedSemaphore, long fence, long commandPool, VkCommandBuffer commandBuffer) {
        this.imageAvailableSemaphore = imageAvailableSemaphore;
        this.renderFinishedSemaphore = renderFinishedSemaphore;
        this.fence = fence;
        this.commandBuffer = commandBuffer;
        this.commandPool = commandPool;
    }

    public Frame() {

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
