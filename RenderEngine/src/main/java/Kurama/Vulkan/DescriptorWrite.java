package Kurama.Vulkan;

public record DescriptorWrite(int dstBinding, int descriptorType, DescriptorBufferInfo bufferInfo, DescriptorImageInfo imageInfo) {

    public DescriptorWrite(int dstBinding, int descriptorType, DescriptorBufferInfo bufferInfo, DescriptorImageInfo imageInfo) {
        this.descriptorType = descriptorType;
        this.dstBinding = dstBinding;
        this.bufferInfo = bufferInfo;
        this.imageInfo = imageInfo;

        if(bufferInfo == null && imageInfo == null) {
            throw new RuntimeException("Both imageInfo and buferInfo cannot be null");
        }

        if(bufferInfo != null && imageInfo != null) {
            throw new RuntimeException("Both imageInfo and buferInfo cannot be not null");
        }
    }

    public DescriptorWrite(int dstBinding, int descriptorType, DescriptorBufferInfo bufferInfo) {
        this(dstBinding, descriptorType, bufferInfo, null);
    }

    public DescriptorWrite(int dstBinding, int descriptorType, DescriptorImageInfo imageInfo) {
        this(dstBinding, descriptorType, null, imageInfo);
    }

}
