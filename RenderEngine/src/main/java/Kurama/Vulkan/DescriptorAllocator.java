package Kurama.Vulkan;

import org.lwjgl.vulkan.VkDescriptorPoolSize;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDevice;

import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;

public class DescriptorAllocator {

    VkDevice device;
    List<Long> freePools;
    List<Long> usedPools;


    private long currentPool;

    public void init(VkDevice device) {
        this.device = device;
    }

    public void cleanUp() {
        for (var p : freePools) {
            vkDestroyDescriptorPool(device, p, null);
        }
        for (var p : usedPools) {
            vkDestroyDescriptorPool(device, p, null);
        }
    }

    public void createPool() {
        
    }

    public void resetPools() {

    }

    public long allocate(long descriptorSetLayout) {
        return 0;
    }

}
