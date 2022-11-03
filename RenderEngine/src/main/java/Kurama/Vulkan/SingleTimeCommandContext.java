package Kurama.Vulkan;

import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkQueue;

import static org.lwjgl.vulkan.VK10.vkDestroyCommandPool;
import static org.lwjgl.vulkan.VK10.vkDestroyFence;

// Used for submitting immediate commands to the gpu
public class SingleTimeCommandContext {
    public long fence;
    public long commandPool;
    public VkCommandBuffer commandBuffer;
    public VkQueue queue;

    public SingleTimeCommandContext() {

    }
    public SingleTimeCommandContext(long fence, long commandPool, VkCommandBuffer commandBuffer, VkQueue queue) {
        this.fence = fence;
        this.commandPool = commandPool;
        this.commandBuffer = commandBuffer;
        this.queue = queue;
    }

    public void cleanUp(VkDevice device) {
        vkDestroyCommandPool(device, this.commandPool, null);
        vkDestroyFence(device, this.fence, null);
    }

}
