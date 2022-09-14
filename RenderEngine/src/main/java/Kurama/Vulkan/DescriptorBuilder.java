package Kurama.Vulkan;

import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.util.List;

public class DescriptorBuilder {

    private List<VkWriteDescriptorSet> writes;
    private List<DescriptorSetLayoutCache.DescriptorBinding> bindings;
    private DescriptorSetLayoutCache cache;
    private DescriptorAllocator allocator;

    public DescriptorBuilder(DescriptorSetLayoutCache layoutCache, DescriptorAllocator descAllocator) {
        this.cache = layoutCache;
        this.allocator = descAllocator;
    }

    public DescriptorBuilder bindBuffer(int binding, VkDescriptorBufferInfo bufferInfo, int descriptorType, int stageFlags) {
        return null;
    }

}
