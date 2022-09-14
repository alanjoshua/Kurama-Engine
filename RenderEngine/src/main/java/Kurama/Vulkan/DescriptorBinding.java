package Kurama.Vulkan;

public class DescriptorBinding {

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