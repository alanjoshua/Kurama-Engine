package Kurama.Vulkan;

import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.HashMap;
import java.util.LinkedList;

import static Kurama.Vulkan.VulkanUtilities.vkCheck;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.EXTDescriptorIndexing.VK_DESCRIPTOR_POOL_CREATE_UPDATE_AFTER_BIND_BIT_EXT;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
import static org.lwjgl.vulkan.VK11.VK_ERROR_OUT_OF_POOL_MEMORY;

public class DescriptorAllocator {

    VkDevice device;
    public LinkedList<Long> freePools = new LinkedList<>();
    public LinkedList<Long> usedPools = new LinkedList<>();

    private Long currentPool = null;

    public HashMap<Integer, Float> poolSizes = new HashMap<>();

    // arbitrary default
    public int poolDescriptorCount = 1000;

    public DescriptorAllocator() {
        poolSizes.put(VK_DESCRIPTOR_TYPE_SAMPLER, 0.5f);
        poolSizes.put(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 100f);
        poolSizes.put(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE, 4f);
        poolSizes.put(VK_DESCRIPTOR_TYPE_STORAGE_IMAGE, 1f);
        poolSizes.put(VK_DESCRIPTOR_TYPE_UNIFORM_TEXEL_BUFFER, 1f);
        poolSizes.put(VK_DESCRIPTOR_TYPE_STORAGE_TEXEL_BUFFER, 1f);
        poolSizes.put(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, 2f);
        poolSizes.put(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, 2f);
        poolSizes.put(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC, 1f);
        poolSizes.put(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER_DYNAMIC, 1f);
        poolSizes.put(VK_DESCRIPTOR_TYPE_INPUT_ATTACHMENT, 0.5f);
    }

    public DescriptorAllocator(HashMap<Integer, Float> poolSizes) {
        this.poolSizes = poolSizes;
    }

    public void init(VkDevice device) {
        this.device = device;
    }

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

    private long grabPool() {

        if(freePools.size() > 0) {
            return freePools.removeLast();
        }

        else {
            return createPool(device, poolSizes, poolDescriptorCount, VK_DESCRIPTOR_POOL_CREATE_UPDATE_AFTER_BIND_BIT_EXT);
        }
    }

    public void resetPools() {

        for(var pool: usedPools) {
            vkResetDescriptorPool(device, pool, 0);
            freePools.add(pool);
        }

        usedPools.clear();
        currentPool = null;
    }

    public long createPool(VkDevice device, HashMap<Integer, Float> poolSizes, int count, int descriptorPoolCreateFlags ) {
        try (var stack = stackPush()) {
            VkDescriptorPoolSize.Buffer creationPoolSizes = VkDescriptorPoolSize.calloc(poolSizes.size(), stack);

            var keys = poolSizes.keySet().toArray();
            for(int i = 0; i < poolSizes.size(); i++) {
                var type = (Integer) keys[i];
                creationPoolSizes.get(i).type(type);
                creationPoolSizes.get(i).descriptorCount((int) (poolSizes.get(type) * count));
            }

            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
            poolInfo.pPoolSizes(creationPoolSizes);
            poolInfo.maxSets(count);
            poolInfo.flags(descriptorPoolCreateFlags);

            LongBuffer pDescriptorPool = stack.mallocLong(1);
            VulkanUtilities.vkCheck(vkCreateDescriptorPool(device, poolInfo, null, pDescriptorPool));

            return pDescriptorPool.get(0);
        }
    }

    public void cleanUp() {
        for (var p : freePools) {
            vkDestroyDescriptorPool(device, p, null);
        }
        for (var p : usedPools) {
            vkDestroyDescriptorPool(device, p, null);
        }
    }

}
