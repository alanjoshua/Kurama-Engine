package Kurama.Vulkan;

import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.util.List;

import static Kurama.Vulkan.VulkanUtilities.createWriteDescriptorSet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class DescriptorBuilder {

    private List<VkWriteDescriptorSet> writes;
    private List<DescriptorSetLayoutCache.DescriptorBinding> bindings;
    private DescriptorSetLayoutCache cache;
    private DescriptorAllocator allocator;
    public record LayoutAndSet(long layout, long descriptorSet) {}

    public DescriptorBuilder(DescriptorSetLayoutCache layoutCache, DescriptorAllocator descAllocator) {
        this.cache = layoutCache;
        this.allocator = descAllocator;
    }

    public LayoutAndSet build() {

        var layout = cache.createDescriptorLayout(bindings);
        var descriptorSet = allocator.allocate(layout);

        VkWriteDescriptorSet.Buffer descriptorWrites;

        try (var stack = stackPush()) {
            descriptorWrites = VkWriteDescriptorSet.calloc(writes.size(), stack);

            for(int i = 0; i < writes.size(); i++) {
                writes.get(i).dstSet(descriptorSet);
                descriptorWrites.put(i, writes.get(i));
            }
        }

        vkUpdateDescriptorSets(allocator.device, descriptorWrites, null);
        return new LayoutAndSet(layout, descriptorSet);
    }

    public DescriptorBuilder bindBuffer(int binding, VkDescriptorBufferInfo.Buffer bufferInfo, int descriptorType, int stageFlags) {

        bindings.add(new DescriptorSetLayoutCache.DescriptorBinding(binding, descriptorType, stageFlags));

        try (var stack = stackPush()) {
            var write = VkWriteDescriptorSet.calloc(stack);
            write.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
            write.dstBinding(binding);
            write.descriptorCount(1);
            write.descriptorType(descriptorType);
            write.pBufferInfo(bufferInfo);
            writes.add(write);
        }
        return this;
    }

    public DescriptorBuilder bindImage(int binding, VkDescriptorImageInfo.Buffer imageInfo, int descriptorType, int stageFlags) {

        bindings.add(new DescriptorSetLayoutCache.DescriptorBinding(binding, descriptorType, stageFlags));

        try (var stack = stackPush()) {
            var write = VkWriteDescriptorSet.calloc(stack);
            write.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
            write.dstBinding(binding);
            write.descriptorCount(1);
            write.descriptorType(descriptorType);
            write.pImageInfo(imageInfo);
            writes.add(write);
        }
        return this;
    }

}
