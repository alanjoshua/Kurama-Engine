package main;

import Kurama.Math.Vector;
import Kurama.Mesh.Mesh;
import Kurama.Mesh.Meshlet;
import Kurama.Vulkan.*;
import Kurama.game.Game;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.function.Consumer;

import static Kurama.Mesh.Mesh.VERTATTRIB.*;
import static Kurama.Mesh.MeshletGen.generateMeshlets;
import static Kurama.Mesh.MeshletGen.mergeMeshes;
import static Kurama.Vulkan.VulkanUtilities.*;
import static Kurama.utils.Logger.log;
import static Kurama.utils.Logger.logPerSec;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.EXTMeshShader.*;
import static org.lwjgl.vulkan.KHRDynamicRendering.*;
import static org.lwjgl.vulkan.KHRDynamicRendering.vkCmdEndRenderingKHR;
import static org.lwjgl.vulkan.KHRShaderFloatControls.VK_KHR_SHADER_FLOAT_CONTROLS_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSpirv14.VK_KHR_SPIRV_1_4_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.KHRSynchronization2.VK_IMAGE_LAYOUT_ATTACHMENT_OPTIMAL_KHR;
import static org.lwjgl.vulkan.NVMeshShader.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MESH_SHADER_FEATURES_NV;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.VK_STRUCTURE_TYPE_RENDER_PASS_MULTIVIEW_CREATE_INFO;

public class PointCloudRenderer extends VulkanRendererBase {

    public boolean windowResized = false;
    public long descriptorSet1Layout;
    public long meshletDescriptorSetLayout;

    public PointCloudController controller;
    public int MAXOBJECTS = 10000;
    public int MAXVERTICES = 1000000;
    public int MAXMESHLETS = 1000000;
    public int GPUObjectData_SIZEOF = Float.BYTES * 16;
    public int VERTEX_SIZE = Float.BYTES * 8;
    public int MESHLETSIZE = (Float.BYTES * 4 * 3);
    public List<Frame> frames = new ArrayList<>();
    public List<Meshlet> meshlets = new ArrayList<>();
    public Map<Mesh.VERTATTRIB, List<Vector>> globalVertAttribs = new HashMap<>();
    public List<Mesh.VERTATTRIB> meshAttribsToLoad = new ArrayList<>(Arrays.asList(Mesh.VERTATTRIB.POSITION));
    public List<Integer> meshletVertexIndexBuffer = new ArrayList<>();
    public List<Integer> meshletLocalIndexBuffer = new ArrayList<>();

    public class Frame {
        public AllocatedBuffer cameraBuffer;
        public AllocatedBuffer objectBuffer;
        public long frameBuffer;
        public AllocatedBuffer vertexBuffer;
        public AllocatedBuffer meshletVertexBuffer;
        public AllocatedBuffer meshletVertexLocalIndexBuffer;
        public AllocatedBuffer meshletCountBuffer;
        public AllocatedBuffer meshletDrawCountBuffer;
        public AllocatedBuffer meshletDescBuffer;
        public long descriptorSet1;
        public long meshletDescriptorSet;
    }

    public PointCloudRenderer(Game game) {
        super(game);

//        DEVICE_EXTENSIONS.add(VK_KHR_GET_PHYSICAL_DEVICE_PROPERTIES_2_EXTENSION_NAME);
        DEVICE_EXTENSIONS.add(VK_EXT_MESH_SHADER_EXTENSION_NAME);
        DEVICE_EXTENSIONS.add(VK_KHR_SPIRV_1_4_EXTENSION_NAME);
        DEVICE_EXTENSIONS.add(VK_KHR_SHADER_FLOAT_CONTROLS_EXTENSION_NAME);

        var meshShaderFeatures = VkPhysicalDeviceMeshShaderFeaturesEXT.calloc();
        meshShaderFeatures.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MESH_SHADER_FEATURES_NV);
        meshShaderFeatures.meshShader(true);
        meshShaderFeatures.taskShader(true);
        createInfo.pNext(meshShaderFeatures);

        this.controller = (PointCloudController) game;

        for(var key: meshAttribsToLoad) {
            globalVertAttribs.put(key, new ArrayList<>());
        }
    }

    @Override
    public void initRenderer() {

        for(int i = 0; i < drawCmds.size(); i++) {
            frames.add(new Frame());
        }
        initBuffers();
        initDescriptorSets();
        createDepthAttachment();
        initFrameBuffers();
        initPipelines();
    }

    public void initFrameBuffers() {

        try(MemoryStack stack = stackPush()) {

            int i = 0;
            for(var swapchainAttach: swapChainAttachments) {

                var attachments = stack.longs(VK_NULL_HANDLE);
                LongBuffer pFramebuffer = stack.mallocLong(1);

                VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.calloc(stack);
                framebufferInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
                framebufferInfo.renderPass(renderPass);
                framebufferInfo.width(swapChainExtent.width());
                framebufferInfo.height(swapChainExtent.height());
                framebufferInfo.layers(1);

                // Create single frameBuffer for multiview renderpass
                framebufferInfo.renderPass(renderPass);
                attachments = stack.longs(swapchainAttach.swapChainImageView, depthAttachment.imageView);
                framebufferInfo.pAttachments(attachments);

                if (vkCreateFramebuffer(device, framebufferInfo, null, pFramebuffer) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create framebuffer");
                }

                frames.get(i).frameBuffer = pFramebuffer.get(0);
                i++;
            }

            frames.forEach(f -> deletionQueue.add(() -> vkDestroyFramebuffer(device, f.frameBuffer, null)));

        }
    }

    public void recordCommandBuffers() {
        try (var stack = stackPush()) {

            for(int i = 0; i < drawCmds.size(); i++) {

                var commandBuffer = drawCmds.get(i);

                vkCheck(vkResetCommandBuffer(commandBuffer, 0), "Failed to begin recording command buffer");

                VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
                beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

                vkCheck(vkBeginCommandBuffer(commandBuffer, beginInfo), "Failed to begin recording command buffer");

                VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc(stack);
                renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO);

                renderPassInfo.renderPass(renderPass);

                VkRect2D renderArea = VkRect2D.calloc(stack);
                renderArea.offset(VkOffset2D.calloc(stack).set(0, 0));
                renderArea.extent(swapChainExtent);
                renderPassInfo.renderArea(renderArea);

                VkClearValue.Buffer clearValues = VkClearValue.calloc(2, stack);
                clearValues.get(0).color().float32(stack.floats(0.0f, 0.0f, 0.0f, 1.0f));
                clearValues.get(1).depthStencil().set(1.0f, 0);

                renderPassInfo.pClearValues(clearValues);

                var viewportBuffer = VkViewport.calloc(1, stack);
                viewportBuffer.width(swapChainExtent.width());
                viewportBuffer.height(swapChainExtent.height());
                viewportBuffer.minDepth(0.0f);
                viewportBuffer.maxDepth(1.0f);

                var scissorBuffer = VkRect2D.calloc(1, stack);
                scissorBuffer.offset(VkOffset2D.calloc(stack).set(0, 0));
                scissorBuffer.extent(swapChainExtent);

                vkCmdSetViewport(commandBuffer, 0, viewportBuffer);
                vkCmdSetScissor(commandBuffer, 0, scissorBuffer);

                log("num of task shader work groups launched: "+ (meshlets.size() /32 + 1));

                renderPassInfo.framebuffer(frames.get(i).frameBuffer);
                vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);

                {
                    vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline);

                    vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                            pipelineLayout, 0,
                            stack.longs(frames.get(i).descriptorSet1), null);

                    vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                            pipelineLayout, 1,
                            stack.longs(frames.get(i).meshletDescriptorSet), null);

                    vkCmdDrawMeshTasksEXT(commandBuffer, meshlets.size()/32 + 1, 1, 1);
                }

                vkCmdEndRenderPass(commandBuffer);

                if (vkEndCommandBuffer(commandBuffer) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to record command buffer");
                }

            }
        }
    }

    @Override
    public void render() {
        prepareFrame();
        drawFrame();

        var bufferReader = new BufferWriter(vmaAllocator, frames.get(currentDisplayBufferIndex).meshletDrawCountBuffer,
                Integer.BYTES, Integer.BYTES);
        bufferReader.mapBuffer();

        var meshletRenderCount = bufferReader.buffer.getInt();

        bufferReader.unmapBuffer();

        logPerSec("Rendered object count: " + meshletRenderCount);

        submitFrame();
    }

    @Override
    public void swapChainRecreatedEvent() {
        recordCommandBuffers();
    }

    public void createMeshlets() {

        for(int modelInd = 0; modelInd < controller.models.size(); modelInd++) {
            var model = controller.models.get(modelInd);

            var results = generateMeshlets(model.meshes.get(0), 3, 64, 124,
                    globalVertAttribs.get(Mesh.VERTATTRIB.POSITION).size(),
                    meshletVertexIndexBuffer.size(), meshletLocalIndexBuffer.size());

//            log("Finished creating meshlets. Num of meshlets: " + results.meshlets().size() +
//                    " for num of prims: "+ mergedMesh.indices.size()/3 +
//                    " num of verts: "+ mergedMesh.vertAttributes.get(Mesh.VERTATTRIB.POSITION).size());

            int ind = modelInd;
            results.meshlets().forEach(meshlet -> meshlet.objectId = ind);
            log("meshlet model ind: "+ind);
            meshlets.addAll(results.meshlets());

            for(var key: meshAttribsToLoad) {
                if(!model.meshes.get(0).vertAttributes.containsKey(key)) {
                    throw new RuntimeException("Mesh "+ model.meshes.get(0).meshLocation + " does not have the required vertex attribute: "+ key);
                }
                globalVertAttribs.get(key).addAll(model.meshes.get(0).vertAttributes.get(key));
            }
            meshletVertexIndexBuffer.addAll(results.vertexIndexBuffer());
            meshletLocalIndexBuffer.addAll(results.localIndexBuffer());

            log(" num of total verts: "+ globalVertAttribs.get(Mesh.VERTATTRIB.POSITION).size());

        }

    }

    @Override
    public void meshesMergedEvent() {
        updateObjectBufferDataInMemory();
        updateVertexAndIndexBuffers();
        updateMeshletInfoBuffer();
        recordCommandBuffers();
    }

    @Override
    public void cameraUpdatedEvent() {
        updateCameraBuffer();
    }

    // TODO: This must be based on updating portions of the list instead of entire portions
    public void updateMeshletInfoBuffer() {

        for(var frame: frames) {
            // Update meshlets desc buffer
            var bw = new BufferWriter(vmaAllocator, frame.meshletDescBuffer, MESHLETSIZE, MESHLETSIZE * meshlets.size());
            bw.mapBuffer();
            for(int i = 0; i < meshlets.size(); i++) {
                bw.setPosition(i);
                bw.put((float)meshlets.get(i).primitiveCount);
                bw.put((float)meshlets.get(i).vertexCount);
                bw.put((float)meshlets.get(i).indexBegin);
                bw.put((float)meshlets.get(i).vertexBegin);

                bw.put(meshlets.get(i).pos);
                bw.put(meshlets.get(i).boundRadius);

                bw.putFloat(meshlets.get(i).objectId);
                bw.putFloat(0);
                bw.putFloat(0);
                bw.putFloat(0);

            }
            bw.unmapBuffer();

            bw = new BufferWriter(vmaAllocator, frame.meshletCountBuffer, Integer.SIZE, Integer.SIZE);
            bw.mapBuffer();
            bw.setPosition(0);
            bw.put(meshlets.size());
            bw.unmapBuffer();
        }
    }

    public void updateVertexAndIndexBuffers() {

        try (var stack = stackPush()) {
            var data = stack.mallocPointer(1);
            ByteBuffer byteBuffer = null;

            // Meshlet Vert indices
            int meshletVertsBufferSize = meshletVertexIndexBuffer.size() * Integer.BYTES;

            var meshletVerticesStagingBuffer = VulkanUtilities.createBufferVMA(vmaAllocator,
                    meshletVertsBufferSize,
                    VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VMA_MEMORY_USAGE_AUTO,
                    VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT);

            vmaMapMemory(vmaAllocator, meshletVerticesStagingBuffer.allocation, data);
            byteBuffer = data.getByteBuffer(0, meshletVertsBufferSize);
            VulkanUtilities.memcpyInt(byteBuffer, meshletVertexIndexBuffer);
            vmaUnmapMemory(vmaAllocator, meshletVerticesStagingBuffer.allocation);

            // Meshlet local indices indices
            int meshletLocalIndicesBufferSize = meshletLocalIndexBuffer.size() * Integer.BYTES;
            log("size of meshletLocalIndicesbuffer: "+meshletLocalIndicesBufferSize);
            var  meshletIndicesStagingBuffer = VulkanUtilities.createBufferVMA(vmaAllocator,
                    meshletLocalIndicesBufferSize,
                    VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VMA_MEMORY_USAGE_AUTO,
                    VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT);

            vmaMapMemory(vmaAllocator, meshletIndicesStagingBuffer.allocation, data);
            byteBuffer = data.getByteBuffer(0, meshletLocalIndicesBufferSize);
            VulkanUtilities.memcpyInt(byteBuffer, meshletLocalIndexBuffer);
            vmaUnmapMemory(vmaAllocator, meshletIndicesStagingBuffer.allocation);

            // GLOBAL VERTEX BUFFER
            var globalVertexBufferSize = globalVertAttribs.get(POSITION).size() * VERTEX_SIZE;
            var  globalVerticesStagingBuffer = VulkanUtilities.createBufferVMA(vmaAllocator,
                    globalVertexBufferSize,
                    VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VMA_MEMORY_USAGE_AUTO,
                    VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT);

            vmaMapMemory(vmaAllocator, globalVerticesStagingBuffer.allocation, data);
            byteBuffer = data.getByteBuffer(0, globalVertexBufferSize);

            for(int i = 0; i < globalVertAttribs.get(POSITION).size(); i++) {
                //pos
                byteBuffer.putFloat(globalVertAttribs.get(POSITION).get(i).get(0));
                byteBuffer.putFloat(globalVertAttribs.get(POSITION).get(i).get(1));
                byteBuffer.putFloat(globalVertAttribs.get(POSITION).get(i).get(2));
                byteBuffer.putFloat(1f);

                // Color - each meshlet will have a random colour
                // error condition
//                if(globalVertAttribs.get(COLOR).size() <= i) {
//                    byteBuffer.putFloat(1f);
//                    byteBuffer.putFloat(1f);
//                    byteBuffer.putFloat(1f);
//                    byteBuffer.putFloat(1f);
//                }
//                else {
                    byteBuffer.putFloat(globalVertAttribs.get(COLOR).get(i).get(0));
                    byteBuffer.putFloat(globalVertAttribs.get(COLOR).get(i).get(1));
                    byteBuffer.putFloat(globalVertAttribs.get(COLOR).get(i).get(2));
                    byteBuffer.putFloat(globalVertAttribs.get(COLOR).get(i).get(3));
//                }

            }
            vmaUnmapMemory(vmaAllocator, globalVerticesStagingBuffer.allocation);

            var vertexCopy = VkBufferCopy.calloc(1, stack);
            var localIndexCopy = VkBufferCopy.calloc(1, stack);
            var meshletVertexCopy = VkBufferCopy.calloc(1, stack);

            for(var frame: frames) {

                Consumer<VkCommandBuffer> copyCmd = cmd -> {

                    vertexCopy.dstOffset(0);
                    vertexCopy.size(globalVertexBufferSize);
                    vertexCopy.srcOffset(0);
                    vkCmdCopyBuffer(cmd, globalVerticesStagingBuffer.buffer, frame.vertexBuffer.buffer, vertexCopy);

                    localIndexCopy.dstOffset(0);
                    localIndexCopy.size(meshletLocalIndicesBufferSize);
                    localIndexCopy.srcOffset(0);
                    vkCmdCopyBuffer(cmd, meshletIndicesStagingBuffer.buffer, frame.meshletVertexLocalIndexBuffer.buffer, localIndexCopy);

                    meshletVertexCopy.dstOffset(0);
                    meshletVertexCopy.size(meshletVertsBufferSize);
                    meshletVertexCopy.srcOffset(0);
                    vkCmdCopyBuffer(cmd, meshletVerticesStagingBuffer.buffer, frame.meshletVertexBuffer.buffer, meshletVertexCopy);
                };

                VulkanUtilities.submitImmediateCommand(copyCmd, singleTimeTransferCommandContext);
            }

        vmaDestroyBuffer(vmaAllocator, globalVerticesStagingBuffer.buffer, globalVerticesStagingBuffer.allocation);
        vmaDestroyBuffer(vmaAllocator, meshletIndicesStagingBuffer.buffer, meshletIndicesStagingBuffer.allocation);
        vmaDestroyBuffer(vmaAllocator, meshletVerticesStagingBuffer.buffer, meshletVerticesStagingBuffer.allocation);

        }

    }

    public void updateObjectBufferDataInMemory() {
        // TODO: Check whether buffer must be resized

        var alignmentSize = GPUObjectData_SIZEOF;
        int bufferSize = alignmentSize * controller.models.size();

        for(var frame: frames) {

            var bw = new BufferWriter(vmaAllocator, frame.objectBuffer, alignmentSize, bufferSize);
            bw.mapBuffer();
            for (int i = 0; i < controller.models.size(); i++) {
                var model = controller.models.get(i);
//                if (renderable.isDirty) {
                    bw.setPosition(i);
                    bw.put(model.objectToWorldMatrix);
//                    renderable.isDirty = false;
//                }
            }

            bw.unmapBuffer();
        }
    }

    @Override
    public void initDescriptorSets() {
        for(var frame: frames) {
            var results = new DescriptorBuilder(descriptorSetLayoutCache, descriptorAllocator)
                    .bindBuffer(0, new DescriptorBufferInfo(0, VK_WHOLE_SIZE, frame.cameraBuffer.buffer),
                            VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, VK_SHADER_STAGE_MESH_BIT_EXT | VK_SHADER_STAGE_TASK_BIT_EXT)
                    .bindBuffer(1, new DescriptorBufferInfo(0, VK_WHOLE_SIZE, frame.objectBuffer.buffer),
                            VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_MESH_BIT_EXT | VK_SHADER_STAGE_TASK_BIT_EXT)
                    .bindBuffer(2, new DescriptorBufferInfo(0, VK_WHOLE_SIZE, frame.vertexBuffer.buffer),
                            VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_MESH_BIT_EXT)
                    .bindBuffer(3, new DescriptorBufferInfo(0, VK_WHOLE_SIZE, frame.meshletDrawCountBuffer.buffer),
                            VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_MESH_BIT_EXT | VK_SHADER_STAGE_TASK_BIT_EXT)
                    .build();

            descriptorSet1Layout = results.layout();
            frame.descriptorSet1 = results.descriptorSet();

            results = new DescriptorBuilder(descriptorSetLayoutCache, descriptorAllocator)
                    .bindBuffer(0, new DescriptorBufferInfo(0, VK_WHOLE_SIZE, frame.meshletDescBuffer.buffer),
                            VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_MESH_BIT_EXT | VK_SHADER_STAGE_TASK_BIT_EXT)
                    .bindBuffer(1, new DescriptorBufferInfo(0, VK_WHOLE_SIZE, frame.meshletVertexBuffer.buffer),
                            VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_MESH_BIT_EXT)
                    .bindBuffer(2, new DescriptorBufferInfo(0, VK_WHOLE_SIZE, frame.meshletVertexLocalIndexBuffer.buffer),
                            VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_MESH_BIT_EXT)
                    .bindBuffer(3, new DescriptorBufferInfo(0, VK_WHOLE_SIZE, frame.meshletCountBuffer.buffer),
                            VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, VK_SHADER_STAGE_MESH_BIT_EXT)
                    .build();

            meshletDescriptorSetLayout = results.layout();
            frame.meshletDescriptorSet = results.descriptorSet();

        }
    }

    @Override
    public void initBuffers() {

        for(var frame: frames) {
            frame.cameraBuffer = createBufferVMA(
                    vmaAllocator,
                    Float.BYTES * ((3 * 16)+(6 * 4)),
                    VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                    VMA_MEMORY_USAGE_AUTO,
                    VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT |
                            VMA_ALLOCATION_CREATE_HOST_ACCESS_ALLOW_TRANSFER_INSTEAD_BIT);

            frame.objectBuffer = createBufferVMA(
                    vmaAllocator,
                    GPUObjectData_SIZEOF * MAXOBJECTS,
                    VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
                    VMA_MEMORY_USAGE_AUTO,
                    VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT |
                            VMA_ALLOCATION_CREATE_HOST_ACCESS_ALLOW_TRANSFER_INSTEAD_BIT
            );

            frame.vertexBuffer = VulkanUtilities.createBufferVMA(vmaAllocator,
                    MAXVERTICES * VERTEX_SIZE,
                    VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
                    VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE, VMA_ALLOCATION_CREATE_DEDICATED_MEMORY_BIT
            );

            var meshletVertIndexBuffSize = Integer.BYTES * MAXVERTICES;

            frame.meshletVertexBuffer = VulkanUtilities.createBufferVMA(vmaAllocator, meshletVertIndexBuffSize,
                    VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
                    VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE, VMA_ALLOCATION_CREATE_DEDICATED_MEMORY_BIT);

            frame.meshletVertexLocalIndexBuffer = VulkanUtilities.createBufferVMA(vmaAllocator, meshletVertIndexBuffSize,
                    VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
                    VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE, VMA_ALLOCATION_CREATE_DEDICATED_MEMORY_BIT);

            frame.meshletCountBuffer = createBufferVMA(vmaAllocator, Integer.SIZE, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                    VMA_MEMORY_USAGE_AUTO, VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT |
                            VMA_ALLOCATION_CREATE_HOST_ACCESS_ALLOW_TRANSFER_INSTEAD_BIT);

            frame.meshletDrawCountBuffer = createBufferVMA(
                    vmaAllocator,
                    Integer.BYTES,
                    VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
                    VMA_MEMORY_USAGE_AUTO,
                    VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT |
                            VMA_ALLOCATION_CREATE_HOST_ACCESS_ALLOW_TRANSFER_INSTEAD_BIT
            );

            var meshletDescBufferSize = MAXMESHLETS * MESHLETSIZE;
            frame.meshletDescBuffer = createBufferVMA(
                    vmaAllocator,
                    meshletDescBufferSize,
                    VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
                    VMA_MEMORY_USAGE_AUTO,
                    VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT |
                            VMA_ALLOCATION_CREATE_HOST_ACCESS_ALLOW_TRANSFER_INSTEAD_BIT
            );

            deletionQueue.add(() -> {
                vmaDestroyBuffer(vmaAllocator, frame.cameraBuffer.buffer, frame.cameraBuffer.allocation);
                vmaDestroyBuffer(vmaAllocator, frame.objectBuffer.buffer, frame.objectBuffer.allocation);
                vmaDestroyBuffer(vmaAllocator, frame.vertexBuffer.buffer, frame.vertexBuffer.allocation);
                vmaDestroyBuffer(vmaAllocator, frame.meshletDescBuffer.buffer, frame.meshletDescBuffer.allocation);
                vmaDestroyBuffer(vmaAllocator, frame.meshletVertexBuffer.buffer, frame.meshletVertexBuffer.allocation);
                vmaDestroyBuffer(vmaAllocator, frame.meshletVertexLocalIndexBuffer.buffer, frame.meshletVertexLocalIndexBuffer.allocation);
                vmaDestroyBuffer(vmaAllocator, frame.meshletDrawCountBuffer.buffer, frame.meshletDrawCountBuffer.allocation);
                vmaDestroyBuffer(vmaAllocator, frame.meshletCountBuffer.buffer, frame.meshletCountBuffer.allocation);
            });
        }
    }

    @Override
    public void initRenderPasses() {
        try(MemoryStack stack = stackPush()) {

            VkAttachmentDescription.Buffer attachments;
            VkAttachmentReference.Buffer attachmentRefs;

            attachments = VkAttachmentDescription.calloc(2, stack);
            attachmentRefs = VkAttachmentReference.calloc(2, stack);

            // Color attachment
            var colorAttachment = attachments.get(0);
            colorAttachment.format(swapChainImageFormat);
            colorAttachment.samples(numOfSamples);
            colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
            colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
            colorAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
            colorAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            colorAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            colorAttachment.finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

            var colorAttachmentRef = attachmentRefs.get(0);
            colorAttachmentRef.attachment(0);
            colorAttachmentRef.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            // Depth-Stencil attachments
            VkAttachmentDescription depthAttachment = attachments.get(1);
            depthAttachment.format(findDepthFormat(physicalDevice));
            depthAttachment.samples(numOfSamples);
            depthAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
            depthAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
            depthAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
            depthAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            depthAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            depthAttachment.finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

            VkAttachmentReference depthAttachmentRef = attachmentRefs.get(1);
            depthAttachmentRef.attachment(1);
            depthAttachmentRef.layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

            VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1, stack);
            subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);
            subpass.colorAttachmentCount(1);
            subpass.pColorAttachments(VkAttachmentReference.calloc(1, stack).put(0, colorAttachmentRef));
            subpass.pDepthStencilAttachment(depthAttachmentRef);

            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack);
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO);
            renderPassInfo.pAttachments(attachments);
            renderPassInfo.pSubpasses(subpass);

            LongBuffer pRenderPass = stack.mallocLong(1);

            if(vkCreateRenderPass(device, renderPassInfo, null, pRenderPass) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create render pass");
            }

            renderPass = pRenderPass.get(0);
            deletionQueue.add(() -> vkDestroyRenderPass(device, renderPass, null));
        }
    }

    @Override
    public void initPipelines() {
        var builder = new PipelineBuilder(PipelineBuilder.PipelineType.GRAPHICS);

        builder.dynamicStates.add(VK_DYNAMIC_STATE_VIEWPORT);
        builder.dynamicStates.add(VK_DYNAMIC_STATE_SCISSOR);
        builder.colorAttachmentImageFormat = swapChainImageFormat;
        builder.descriptorSetLayouts = new long[]{descriptorSet1Layout, meshletDescriptorSetLayout};

        builder.shaderStages.add(new PipelineBuilder.ShaderStageCreateInfo("shaders/meshshader.task", VK_SHADER_STAGE_TASK_BIT_EXT));
        builder.shaderStages.add(new PipelineBuilder.ShaderStageCreateInfo("shaders/meshshader.mesh", VK_SHADER_STAGE_MESH_BIT_EXT));
        builder.shaderStages.add(new PipelineBuilder.ShaderStageCreateInfo("shaders/meshshader.frag", VK_SHADER_STAGE_FRAGMENT_BIT));

        builder.depthStencil = new PipelineBuilder.PipelineDepthStencilStateCreateInfo(true, true, VK_COMPARE_OP_LESS_OR_EQUAL, false, false);
//        builder.rasterizer = new PipelineBuilder.PipelineRasterizationStateCreateInfo(VK_CULL_MODE_NONE, VK_FRONT_FACE_COUNTER_CLOCKWISE);
        builder.rasterizer = new PipelineBuilder.PipelineRasterizationStateCreateInfo(VK_CULL_MODE_BACK_BIT, VK_FRONT_FACE_COUNTER_CLOCKWISE);
        builder.inputAssemblyCreateInfo = null;

        var pipeLineCreateResults = builder.build(device, renderPass);
        pipelineLayout = pipeLineCreateResults.pipelineLayout();
        graphicsPipeline = pipeLineCreateResults.pipeline();

        deletionQueue.add(() -> vkDestroyPipeline(device, graphicsPipeline, null));
        deletionQueue.add(() -> vkDestroyPipelineLayout(device, pipelineLayout, null));
    }

    public void updateCameraBuffer() {
        var alignment = Float.BYTES * ((3 * 16) + (6 * 4));
        var bufferSize = alignment;

        var projMat = controller.mainCamera.getPerspectiveProjectionMatrix();
        projMat.getData()[1][1] *= -1;
        var camViewMat = controller.mainCamera.worldToObject;
        var projView = projMat.matMul(camViewMat);

        var frustumPlanes = Arrays.asList(((PointCloudController)game).mainCamera.frustumIntersection.planes);

        for(var frame: frames) {
            var bw = new BufferWriter(vmaAllocator, frame.cameraBuffer, alignment, bufferSize);
            bw.mapBuffer();

            projView.setValuesToBuffer(bw.buffer);
            projMat.setValuesToBuffer(bw.buffer);
            projView.setValuesToBuffer(bw.buffer);

//            log("dot products:");
            var pos = new Vector(1,1,1,1);
            for(var f: frustumPlanes) {
                f.setValuesToBuffer(bw.buffer);
//                log(f + "=" +pos.dot(f));
            }

            bw.unmapBuffer();
        }
    }

    @Override
    public void cleanUp() {
        // Wait for the device to complete all operations before release resources
        vkDeviceWaitIdle(device);

        cleanUpSwapChainAndSwapImages();

        for(int i = deletionQueue.size()-1; i >= 0; i--) {
            deletionQueue.get(i).run();
        }
    }
}
