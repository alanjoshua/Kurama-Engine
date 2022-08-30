package Kurama.Vulkan;

import Kurama.ComponentSystem.components.model.Model;
import Kurama.Mesh.Material;
import Kurama.Mesh.Mesh;
import main.RenderingEngineVulkan;
import main.Vertex;
import org.lwjgl.vulkan.VkBufferCopy;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkQueue;

import java.util.function.Consumer;

import static Kurama.Vulkan.VulkanUtilities.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.util.vma.Vma.vmaUnmapMemory;
import static org.lwjgl.vulkan.VK10.*;

// A renderable object will contain one mesh, one model, and the relevant vulkan specific render attributes
public class Renderable {

    public AllocatedBuffer vertexBuffer;
    public AllocatedBuffer indexBuffer;

    // assume each mesh only has one material
    public Mesh mesh;
    public Model model;


    public Renderable(Mesh mesh, Model model) {
        this.mesh = mesh;
        this.model = model;
    }

    // assume each mesh only has one material
    public Material getMaterial() {
        return mesh.materials.get(0);
    }

    public void uploadMesh(long vmaAllocator, VkQueue queue, RenderingEngineVulkan.SingleTimeCommandObj singleTimeCommandObj) {
        createVertexBuffer(vmaAllocator, queue, singleTimeCommandObj);
        createIndexBuffer(vmaAllocator, queue, singleTimeCommandObj);
    }

    public void createIndexBuffer(long vmaAllocator, VkQueue queue, RenderingEngineVulkan.SingleTimeCommandObj singleTimeCommandObj) {
        try (var stack = stackPush()) {

            var bufferSize = Short.SIZE * mesh.indices.size();
            var stagingBuffer = createBufferVMA(vmaAllocator,
                    bufferSize,
                    VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VMA_MEMORY_USAGE_AUTO,
                    VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT);

            var data = stack.mallocPointer(1);

            vmaMapMemory(vmaAllocator, stagingBuffer.allocation, data);
            {
                memcpyInt(data.getByteBuffer(0, (int) bufferSize),mesh.indices);
            }
            vmaUnmapMemory(vmaAllocator, stagingBuffer.allocation);

            indexBuffer = createBufferVMA(vmaAllocator, bufferSize,
                    VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
                    VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE, VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT);

            Consumer<VkCommandBuffer> copyCmd = cmd -> {
                var copy = VkBufferCopy.calloc(1, stack);
                copy.dstOffset(0);
                copy.srcOffset(0);
                copy.size(bufferSize);
                vkCmdCopyBuffer(cmd, stagingBuffer.buffer, vertexBuffer.buffer, copy);
            };

            submitImmediateCommand(copyCmd, singleTimeCommandObj, queue);

            vmaDestroyBuffer(vmaAllocator, stagingBuffer.buffer, stagingBuffer.allocation);
        }
    }

    public void createVertexBuffer(long vmaAllocator, VkQueue queue, RenderingEngineVulkan.SingleTimeCommandObj singleTimeCommandObj) {
        try (var stack = stackPush()) {

            var bufferSize = Vertex.SIZEOF * mesh.getVertices().size();
            var stagingBuffer = createBufferVMA(vmaAllocator,
                    bufferSize,
                    VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VMA_MEMORY_USAGE_AUTO,
                    VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT);

            var data = stack.mallocPointer(1);

            vmaMapMemory(vmaAllocator, stagingBuffer.allocation, data);
            {
                memcpy(data.getByteBuffer(0, bufferSize), mesh);
            }
            vmaUnmapMemory(vmaAllocator, stagingBuffer.allocation);

            vertexBuffer = createBufferVMA(vmaAllocator, bufferSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                    VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE, VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT);


            Consumer<VkCommandBuffer> copyCmd = cmd -> {
                var copy = VkBufferCopy.calloc(1, stack);
                copy.dstOffset(0);
                copy.srcOffset(0);
                copy.size(bufferSize);
                vkCmdCopyBuffer(cmd, stagingBuffer.buffer, vertexBuffer.buffer, copy);
            };

            submitImmediateCommand(copyCmd, singleTimeCommandObj, queue);

            vmaDestroyBuffer(vmaAllocator, stagingBuffer.buffer, stagingBuffer.allocation);

        }
    }

    public void cleanUp(long vmaAllocator) {
        model.meshes.get(0).materials.get(0).texture.cleanUp();
        vmaDestroyBuffer(vmaAllocator, vertexBuffer.buffer, vertexBuffer.allocation);
        vmaDestroyBuffer(vmaAllocator, indexBuffer.buffer, indexBuffer.allocation);
    }

}
