package Kurama.Vulkan;

import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.util.ArrayList;
import java.util.List;

import static Kurama.Vulkan.VulkanUtilities.createWriteDescriptorSet;
import static Kurama.utils.Logger.log;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class DescriptorBuilder {

    private List<DescriptorWrite> writes = new ArrayList<>();
    private List<DescriptorBinding> bindings = new ArrayList<>();
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

        try (var stack = stackPush()) {
            var descriptorWrites = VkWriteDescriptorSet.calloc(writes.size(), stack);

            for(int i = 0; i < writes.size(); i++) {

                var interWrite = writes.get(i);
                var write = descriptorWrites.get(i);

                write.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                write.dstBinding(interWrite.dstBinding());
                write.dstSet(descriptorSet);
                write.descriptorType(interWrite.descriptorType());
                write.descriptorCount(1);

                if(interWrite.bufferInfo() != null) {
                    var bufferInfo = VkDescriptorBufferInfo.calloc(1, stack);
                    bufferInfo.offset(interWrite.bufferInfo().offset());
                    bufferInfo.range(interWrite.bufferInfo().range());
                    bufferInfo.buffer(interWrite.bufferInfo().buffer());
                    write.pBufferInfo(bufferInfo);
                }

                if(interWrite.imageInfo() != null) {
                    var imageInfo = VkDescriptorImageInfo.calloc(1, stack);
                    imageInfo.sampler(interWrite.imageInfo().sampler());
                    imageInfo.imageView(interWrite.imageInfo().imageView());
                    imageInfo.imageLayout(interWrite.imageInfo().imageLayout());
                    write.pImageInfo(imageInfo);
                }

                descriptorWrites.put(i, write);
            }

            vkUpdateDescriptorSets(allocator.device, descriptorWrites, null);
            return new LayoutAndSet(layout, descriptorSet);
        }
    }

    public DescriptorBuilder bindBuffer(int binding, DescriptorBufferInfo bufferInfo, int descriptorType, int stageFlags) {
        bindings.add(new DescriptorBinding(binding, descriptorType, stageFlags));
        writes.add(new DescriptorWrite(binding, descriptorType, bufferInfo));
        return this;
    }

    public DescriptorBuilder bindImage(int binding, DescriptorImageInfo imageInfo, int descriptorType, int stageFlags) {

        bindings.add(new DescriptorBinding(binding, descriptorType, stageFlags));
        writes.add(new DescriptorWrite(binding, descriptorType, imageInfo));
        return this;
    }

}
