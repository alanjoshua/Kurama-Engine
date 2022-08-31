package Kurama.Vulkan;

import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;

import static org.lwjgl.vulkan.VK10.vkDestroyCommandPool;
import static org.lwjgl.vulkan.VK10.vkDestroyFence;

// Used for submitting immediate commands to the gpu
public class SingleTimeCommandContext {
    public long fence;
    public long commandPool;
    public VkCommandBuffer commandBuffer;

    public SingleTimeCommandContext() {

    }
    public SingleTimeCommandContext(long fence, long commandPool, VkCommandBuffer commandBuffer) {
        this.fence = fence;
        this.commandPool = commandPool;
        this.commandBuffer = commandBuffer;
    }

    public void cleanUp(VkDevice device) {
        vkDestroyCommandPool(device, this.commandPool, null);
        vkDestroyFence(device, this.fence, null);
    }

}
