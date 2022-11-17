package Kurama.Vulkan;

import Kurama.Mesh.Texture;
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
    public AllocatedImage imageBuffer;
    public long textureSampler;
    public long textureImageView;
    public int mipLevels;

    public TextureVK(String fileName) {
        this.fileName = fileName;
    }

    public static void createTextureImage(long vmaAllocator, VkPhysicalDevice physicalDevice, SingleTimeCommandContext singleTimeCommandContext, TextureVK texture) {

        try(var stack = stackPush()) {

            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            IntBuffer pChannels = stack.mallocInt(1);

            ByteBuffer pixels = stbi_load(texture.fileName, pWidth, pHeight, pChannels, STBI_rgb_alpha);

            long imageSize = pWidth.get(0) * pHeight.get(0) * 4;
            texture.mipLevels = (int) Math.floor(Math.log(Math.max(pWidth.get(0), pHeight.get(0)))) + 1;

            if(pixels == null) {
                throw new RuntimeException("Failed to load texture image ");
            }

            var stagingBuffer = createBufferVMA(vmaAllocator,
                    imageSize,
                    VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VMA_MEMORY_USAGE_AUTO, VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT);

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
                    VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT,
                    extent,
                    texture.mipLevels,
                    VK_IMAGE_TILING_OPTIMAL,
                    1,
                    VK_SAMPLE_COUNT_1_BIT,
                    stack);

            var memoryAllocInfo = VmaAllocationCreateInfo.calloc(stack)
                    .usage(VMA_MEMORY_USAGE_AUTO)
                    .requiredFlags(VMA_ALLOCATION_CREATE_DEDICATED_MEMORY_BIT);

            texture.imageBuffer = createImage(imageInfo, memoryAllocInfo, vmaAllocator);
            texture.id = texture.imageBuffer.image;

            Consumer<VkCommandBuffer> transition_copyBuffer_generateMipMap = (cmd) -> {

                transitionImageLayout(texture.imageBuffer.image,
                        imageFormat,
                        VK_IMAGE_LAYOUT_UNDEFINED,
                        VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                        texture.mipLevels, 1,
                        cmd);

                copyBufferToImage(stagingBuffer.buffer, texture.imageBuffer.image, cmd, extent);

                generateMipMaps(texture.imageBuffer.image, imageFormat, pWidth.get(0), pHeight.get(0), texture.mipLevels, physicalDevice, cmd);

            };
            submitImmediateCommand(transition_copyBuffer_generateMipMap, singleTimeCommandContext);

           vmaDestroyBuffer(vmaAllocator, stagingBuffer.buffer, stagingBuffer.allocation);
        }
    }

    public static void createTextureImageView(TextureVK texture) {
        try (var stack = stackPush()) {

            var viewInfo =
                    createImageViewCreateInfo(
                            VK_FORMAT_R8G8B8A8_SRGB,
                            texture.imageBuffer.image,
                            VK_IMAGE_ASPECT_COLOR_BIT,
                            texture.mipLevels,
                            1,
                            VK_IMAGE_VIEW_TYPE_2D,
                            stack
                    );
            texture.textureImageView = createImageView(viewInfo, device);
        }
    }

    public static long createTextureSampler(int maxLod) {
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
            samplerInfo.maxLod(maxLod);
            samplerInfo.mipLodBias(0);

            LongBuffer pTextureSampler = stack.mallocLong(1);

            if(vkCreateSampler(device, samplerInfo, null, pTextureSampler) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create texture sampler");
            }

            var sampler = pTextureSampler.get(0);

            return pTextureSampler.get(0);
        }
    }


    public static void generateMipMaps(long image, int imageFormat, int texWidth, int texHeight, int mipLevels, VkPhysicalDevice physicalDevice, VkCommandBuffer commandBuffer) {
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
                        VK_PIPELINE_STAGE_TRANSFER_BIT,
                        VK_PIPELINE_STAGE_TRANSFER_BIT,
                        0,
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

    }

}
