package Kurama.Vulkan;

import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo;
import org.lwjgl.vulkan.VkDescriptorSetVariableDescriptorCountAllocateInfoEXT;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.EXTDescriptorIndexing.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_VARIABLE_DESCRIPTOR_COUNT_ALLOCATE_INFO_EXT;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.vkAllocateDescriptorSets;
import static org.lwjgl.vulkan.VK11.VK_ERROR_OUT_OF_POOL_MEMORY;

public class BindlessTextureDescriptorAllocator extends DescriptorAllocator {

    public int maxBindlessResources;

    public BindlessTextureDescriptorAllocator(int maxBindlessResources) {
        this.maxBindlessResources = maxBindlessResources;
        this.poolDescriptorCount = 1;
        poolSizes.put(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, (float) maxBindlessResources);
    }

    @Override
    public Long allocate(long descriptorSetLayout) {

        try (var stack = stackPush()) {

            if (currentPool == null) {
                currentPool = grabPool();
                usedPools.add(currentPool);
            }

            var allocinfo = VkDescriptorSetAllocateInfo.calloc(stack);
            allocinfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
            allocinfo.descriptorPool(currentPool);
            allocinfo.pSetLayouts(stack.longs(descriptorSetLayout));


            var countInfo = VkDescriptorSetVariableDescriptorCountAllocateInfoEXT.calloc(stack);
            countInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_VARIABLE_DESCRIPTOR_COUNT_ALLOCATE_INFO_EXT);
            countInfo.pDescriptorCounts(stack.ints(maxBindlessResources-1));
            allocinfo.pNext(countInfo);

            var pDescriptorSet = stack.mallocLong(1);
            var result = vkAllocateDescriptorSets(device, allocinfo, pDescriptorSet);

            if(result == VK_SUCCESS) {
                return pDescriptorSet.get(0);
            }

            // allocate new pool and reallocate descriptor set
            else if(result == VK_ERROR_FRAGMENTED_POOL || result == VK_ERROR_OUT_OF_POOL_MEMORY) {

                currentPool = grabPool();
                usedPools.add(currentPool);
                allocinfo.descriptorPool(currentPool);

                // throw error if allocation still fails
                VulkanUtilities.vkCheck(vkAllocateDescriptorSets(device, allocinfo, pDescriptorSet));

                return pDescriptorSet.get(0);
            }

            // for all other results, throw
            else {
                throw new RuntimeException("Failed to allocate descriptor set");
            }

        }

    }

}
