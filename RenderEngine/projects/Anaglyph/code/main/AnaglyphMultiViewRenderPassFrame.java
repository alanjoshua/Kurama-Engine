package main;

import Kurama.Vulkan.AllocatedBuffer;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.LongBuffer;

import static Kurama.Vulkan.VulkanUtilities.device;
import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.util.vma.Vma.vmaDestroyBuffer;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Wraps the needed sync objects for an in flight frame
 *
 * This frame's sync objects must be deleted manually
 * */
public class AnaglyphMultiViewRenderPassFrame {

    public long semaphore;
    public long fence;
    public long commandPool;
    public VkCommandBuffer commandBuffer;
    public AllocatedBuffer cameraBuffer;
    public AllocatedBuffer objectBuffer;

    // Global Descriptor set contains the camera data and other scene parameters
    public long cameraAndSceneDescriptorSet;

    // This contains the object transformation matrices
    public long objectDescriptorSet;

    public long vmaAllocator;

    public void cleanUp() {
        vkDestroySemaphore(device, semaphore, null);
        vkDestroyFence(device, fence(), null);
        vkDestroyCommandPool(device, commandPool, null);
        vmaDestroyBuffer(vmaAllocator, cameraBuffer.buffer, cameraBuffer.allocation);
        vmaDestroyBuffer(vmaAllocator, objectBuffer.buffer, objectBuffer.allocation);
    }

    public LongBuffer pSemaphore() {
        return stackGet().longs(semaphore);
    }

    public long fence() {
        return fence;
    }
    public LongBuffer pFence() {
        return stackGet().longs(fence);
    }
}