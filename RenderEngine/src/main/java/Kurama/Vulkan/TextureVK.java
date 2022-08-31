package Kurama.Vulkan;

import Kurama.Mesh.Texture;
import main.RenderingEngineVulkan;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.function.Consumer;

import static Kurama.Vulkan.VulkanUtilities.*;
import static Kurama.Vulkan.VulkanUtilities.memcpy;
import static Kurama.utils.Logger.log;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.VK10.*;

public class TextureVK extends Texture {

    public long id; // same as image.image
    public AllocatedImage image;
    public long textureSampler;
    public long textureImageView;
    public int mipLevels;

    public TextureVK(String fileName) {
        this.fileName = fileName;
    }

    public TextureVK(ByteBuffer buf) {
    }

    public void createTextureImage(VkQueue queue, long vmaAllocator, SingleTimeCommandContext singleTimeCommandContext) {

        try(var stack = stackPush()) {

            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            IntBuffer pChannels = stack.mallocInt(1);

            ByteBuffer pixels = stbi_load(fileName, pWidth, pHeight, pChannels, STBI_rgb_alpha);

            long imageSize = pWidth.get(0) * pHeight.get(0) * 4;
            mipLevels = (int) Math.floor(Math.log(Math.max(pWidth.get(0), pHeight.get(0)))) + 1;

            if(pixels == null) {
                throw new RuntimeException("Failed to load texture image ");
            }

            var stagingBuffer = createBufferVMA(vmaAllocator,
                    imageSize,
                    VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VMA_MEMORY_USAGE_CPU_COPY, null);

            PointerBuffer data = stack.mallocPointer(1);
            vmaMapMemory(vmaAllocator, stagingBuffer.allocation, data);
            {
                memcpy(data.getByteBuffer(0, (int) imageSize), pixels, imageSize);
            }
            vmaUnmapMemory(vmaAllocator, stagingBuffer.allocation);

            stbi_image_free(pixels);

            int imageFormat = VK_FORMAT_R8G8B8A8_SRGB;
            var extent = VkExtent3D.calloc(stack).width(pWidth.get(0)).height(pHeight.get(0)).depth(1);

            var imageInfo = createImageCreateInfo(
                    imageFormat,
                    VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT,
                    extent,
                    mipLevels,
                    VK_IMAGE_TILING_OPTIMAL,
                    VK_SAMPLE_COUNT_1_BIT,
                    stack);

            var memoryAllocInfo = VmaAllocationCreateInfo.calloc(stack)
                    .usage(VMA_MEMORY_USAGE_GPU_ONLY)
                    .requiredFlags(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

            image = createImage(imageInfo, memoryAllocInfo, vmaAllocator);
            id = image.image;

            Consumer<VkCommandBuffer> transition_copyBuffer_generateMipMap = (cmd) -> {

                transitionImageLayout(id,
                        imageFormat,
                        VK_IMAGE_LAYOUT_UNDEFINED,
                        VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                        mipLevels,
                        cmd);

                copyBufferToImage(stagingBuffer.buffer, image.image, cmd, extent);

                generateMipMaps(image.image, imageFormat, pWidth.get(0), pHeight.get(0), mipLevels, cmd);

            };
            submitImmediateCommand(transition_copyBuffer_generateMipMap, singleTimeCommandContext, queue);

           vmaDestroyBuffer(vmaAllocator, stagingBuffer.buffer, stagingBuffer.allocation);
        }
    }

    public void createTextureImageView() {
        try (var stack = stackPush()) {

            var viewInfo =
                    createImageViewCreateInfo(
                            VK_FORMAT_R8G8B8A8_SRGB,
                            image.image,
                            VK_IMAGE_ASPECT_COLOR_BIT,
                            mipLevels,
                            stack
                    );
            textureImageView = createImageView(viewInfo, device);
        }
    }

    public void createTextureSampler() {
        try(MemoryStack stack = stackPush()) {

            VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.calloc(stack);
            samplerInfo.sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO);
            samplerInfo.magFilter(VK_FILTER_LINEAR);
            samplerInfo.minFilter(VK_FILTER_LINEAR);
            samplerInfo.addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT);
            samplerInfo.addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT);
            samplerInfo.addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT);
            samplerInfo.anisotropyEnable(true);
            samplerInfo.maxAnisotropy(16.0f);
            samplerInfo.borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK);
            samplerInfo.unnormalizedCoordinates(false);
            samplerInfo.compareEnable(false);
            samplerInfo.compareOp(VK_COMPARE_OP_ALWAYS);
            samplerInfo.mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR);
            samplerInfo.minLod(0);
            samplerInfo.maxLod(mipLevels);
            samplerInfo.mipLodBias(0);

            LongBuffer pTextureSampler = stack.mallocLong(1);

            if(vkCreateSampler(device, samplerInfo, null, pTextureSampler) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create texture sampler");
            }

            textureSampler = pTextureSampler.get(0);
        }
    }


    public static void generateMipMaps(long image, int imageFormat, int texWidth, int texHeight, int mipLevels, VkCommandBuffer commandBuffer) {
        try (var stack = stackPush()) {

            // Check if image format supports linear blitting
            var formatProperties = VkFormatProperties.malloc(stack);
            vkGetPhysicalDeviceFormatProperties(physicalDevice, imageFormat, formatProperties);

            if((formatProperties.optimalTilingFeatures() & VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT) == 0) {
                throw new RuntimeException("Texture image format does not support linear blitting");
            }

            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack);
            barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
            barrier.image(image);
            barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            barrier.subresourceRange().baseArrayLayer(0);
            barrier.subresourceRange().layerCount(1);
            barrier.subresourceRange().levelCount(1);

            int mipWidth = texWidth;
            int mipHeight = texHeight;
            log("mipmap levels: "+mipLevels);
            for(int i = 1; i < mipLevels; i++) {
                barrier.subresourceRange().baseMipLevel(i-1);
                barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
                barrier.newLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
                barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                barrier.dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT);

                vkCmdPipelineBarrier(commandBuffer,
                        VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0,
                        null,
                        null,
                        barrier);

                VkImageBlit.Buffer blit = VkImageBlit.calloc(1, stack);
                blit.srcOffsets(0).set(0,0,0);
                blit.srcOffsets(1).set(mipWidth, mipHeight, 1);
                blit.srcSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                blit.srcSubresource().mipLevel(i-1);
                blit.srcSubresource().baseArrayLayer(0);
                blit.srcSubresource().layerCount(1);
                blit.dstOffsets(0).set(0,0,0);
                blit.dstOffsets(1).set(mipWidth > 1 ? mipWidth / 2 : 1, mipHeight > 1 ? mipHeight / 2 : 1, 1);
                blit.dstSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                blit.dstSubresource().mipLevel(i);
                blit.dstSubresource().baseArrayLayer(0);
                blit.dstSubresource().layerCount(1);

                vkCmdBlitImage(commandBuffer,
                        image, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                        image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                        blit, VK_FILTER_LINEAR);

                barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
                barrier.newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
                barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);

                vkCmdPipelineBarrier(commandBuffer,
                        VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0,
                        null,null, barrier);

                if(mipWidth > 1) {
                    mipWidth /= 2;
                }

                if(mipHeight > 1) {
                    mipHeight /= 2;
                }
            }
            barrier.subresourceRange().baseMipLevel(mipLevels - 1);
            barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
            barrier.newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
            barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);

            vkCmdPipelineBarrier(commandBuffer,
                    VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0,
                    null,null, barrier);

        }
    }

    public static void copyBufferToImage(long buffer, long image, VkCommandBuffer cmd,VkExtent3D extent) {

        try (var stack = stackPush()) {
            VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack);
            region.bufferOffset(0);
            region.bufferRowLength(0);   // Tightly packed
            region.bufferImageHeight(0);  // Tightly packed

            region.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            region.imageSubresource().mipLevel(0);
            region.imageSubresource().baseArrayLayer(0);
            region.imageSubresource().layerCount(1);
            region.imageOffset().set(0, 0, 0);
            region.imageExtent(extent);

            vkCmdCopyBufferToImage(cmd, buffer, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region);
        }

    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void cleanUp() {
        vkDestroyImageView(device, textureImageView, null);
        vkDestroySampler(device, textureSampler, null);
        vkDestroyImage(device, id, null);
//        vkFreeMemory(device, textureImageMemory, null);
    }

}
