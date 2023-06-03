package Kurama.Vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBindingFlagsCreateInfoEXT;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;
import org.lwjgl.vulkan.VkDevice;

import java.nio.LongBuffer;
import java.util.HashMap;
import java.util.List;

import static Kurama.Vulkan.VulkanUtilities.vkCheck;
import static Kurama.utils.Logger.log;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.EXTDescriptorIndexing.*;
import static org.lwjgl.vulkan.VK10.*;

public class DescriptorSetLayoutCache {

    public VkDevice device;

    public HashMap<List<DescriptorBinding>, Long> layoutCache = new HashMap<>();

    public void init(VkDevice device) {
        this.device = device;
    }

    public long createDescriptorLayout(List<DescriptorBinding> setBindings, boolean shouldSupportBindlessTextureArray) {

        var descriptorSetLayout = layoutCache.get(setBindings);
        if(descriptorSetLayout == null) {

            try (var stack = stackPush()) {

                VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack);

                VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(setBindings.size(), stack);
                for (int i = 0; i < setBindings.size(); i++) {
                    bindings.put(i, createDescriptorSetLayoutBinding(setBindings.get(i).binding, setBindings.get(i).descriptorType, setBindings.get(i).stageFlags, setBindings.get(i).descriptorCount, stack));
                }

                layoutInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
                layoutInfo.pBindings(bindings);

                if(shouldSupportBindlessTextureArray) {
                    layoutInfo.flags(VK_DESCRIPTOR_SET_LAYOUT_CREATE_UPDATE_AFTER_BIND_POOL_BIT_EXT);
                    var extendedInfo = VkDescriptorSetLayoutBindingFlagsCreateInfoEXT.calloc(stack);
                    extendedInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_BINDING_FLAGS_CREATE_INFO_EXT);
                    extendedInfo.bindingCount(setBindings.size());
                    extendedInfo.pBindingFlags(stack.ints(VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT_EXT,
                            VK_DESCRIPTOR_BINDING_VARIABLE_DESCRIPTOR_COUNT_BIT_EXT,
                            VK_DESCRIPTOR_BINDING_UPDATE_AFTER_BIND_BIT_EXT));
                    layoutInfo.pNext(extendedInfo);
                }

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

    public static VkDescriptorSetLayoutBinding createDescriptorSetLayoutBinding(int binding, int descriptorType, int stageFlags, int descriptorCount, MemoryStack stack) {
        VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(1, stack);

        VkDescriptorSetLayoutBinding bufferBinding = bindings.get(0);
        bufferBinding.binding(binding);
        bufferBinding.descriptorCount(descriptorCount);
        bufferBinding.descriptorType(descriptorType);
        bufferBinding.stageFlags(stageFlags);

        return bufferBinding;
    }

}
