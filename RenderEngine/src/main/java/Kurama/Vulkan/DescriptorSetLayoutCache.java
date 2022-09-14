package Kurama.Vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;
import org.lwjgl.vulkan.VkDevice;

import java.nio.LongBuffer;
import java.util.HashMap;
import java.util.List;

import static Kurama.Vulkan.VulkanUtilities.vkCheck;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

public class DescriptorSetLayoutCache {

    public VkDevice device;

    private HashMap<List<DescriptorBinding>, Long> layoutCache = new HashMap<>();

    public void init(VkDevice device) {
        this.device = device;
    }

    public long createDescriptorLayout(List<DescriptorBinding> setBindings) {

        var descriptorSetLayout = layoutCache.get(setBindings);
        if(descriptorSetLayout == null) {

            try (var stack = stackPush()) {

                VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(setBindings.size(), stack);
                for (int i = 0; i < setBindings.size(); i++) {
                    bindings.put(i, createDescriptorSetLayoutBinding(setBindings.get(i).binding, setBindings.get(i).descriptorType, setBindings.get(i).stageFlags, stack));
                }

                VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack);
                layoutInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
                layoutInfo.pBindings(bindings);

                LongBuffer pDescriptorSetLayout = stack.mallocLong(1);
                vkCheck(vkCreateDescriptorSetLayout(device, layoutInfo, null, pDescriptorSetLayout));
                descriptorSetLayout = pDescriptorSetLayout.get(0);

                layoutCache.put(setBindings, descriptorSetLayout);
            }

        }

        return descriptorSetLayout;
    }

    public void cleanUp() {
        for(var key: layoutCache.keySet()) {
            vkDestroyDescriptorSetLayout(device, layoutCache.get(key), null);
        }
        layoutCache.clear();
    }

    public static VkDescriptorSetLayoutBinding createDescriptorSetLayoutBinding(int binding, int descriptorType, int stageFlags, MemoryStack stack) {
        VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(1, stack);

        VkDescriptorSetLayoutBinding bufferBinding = bindings.get(0);
        bufferBinding.binding(binding);
        bufferBinding.descriptorCount(1);
        bufferBinding.descriptorType(descriptorType);
        bufferBinding.stageFlags(stageFlags);

        return bufferBinding;
    }

    public static class DescriptorBinding {

        int binding;
        int descriptorType;
        int stageFlags;

        public DescriptorBinding(int binding, int descriptorType, int stageFlags) {
            this.binding = binding;
            this.descriptorType = descriptorType;
            this.stageFlags = stageFlags;
        }

        @Override
        public int hashCode() {
            // Adapted from hashCode function for Java List
            int hashCode = 1;
            hashCode = 31*hashCode + Integer.valueOf(binding).hashCode();
            hashCode = 31*hashCode + Integer.valueOf(descriptorType).hashCode();
            hashCode = 31*hashCode + Integer.valueOf(stageFlags).hashCode();
            return hashCode;
        }

        @Override
        public boolean equals(Object o) {
            var right = (DescriptorBinding)o;
            if(binding == right.binding
                    && descriptorType == right.descriptorType
                    && stageFlags == right.stageFlags) {
                return true;
            }
            return false;
        }

    }

}
