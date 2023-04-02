package main;

import Kurama.ComponentSystem.components.model.Model;
import Kurama.ComponentSystem.components.model.PointCloud;
import Kurama.Math.Vector;
import Kurama.Mesh.Mesh;
import Kurama.Mesh.Meshlet;
import Kurama.Mesh.MeshletGen;
import Kurama.Vulkan.*;
import Kurama.game.Game;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.function.Consumer;

import static Kurama.Mesh.Mesh.VERTATTRIB.*;
import static Kurama.Mesh.MeshletGen.*;
import static Kurama.Vulkan.VulkanUtilities.*;
import static Kurama.utils.Logger.log;
import static Kurama.utils.Logger.logPerSec;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.EXTMeshShader.*;
import static org.lwjgl.vulkan.KHRShaderFloatControls.VK_KHR_SHADER_FLOAT_CONTROLS_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSpirv14.VK_KHR_SPIRV_1_4_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.NVMeshShader.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MESH_SHADER_FEATURES_NV;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_1_FEATURES;
import static org.lwjgl.vulkan.VK12.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES;
import static org.lwjgl.vulkan.VK13.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_3_FEATURES;

public class PointCloudRenderer extends VulkanRendererBase {

    public boolean windowResized = false;
    public long descriptorSet1Layout;
    public long meshletDescriptorSetLayout;

    public PointCloudController controller;
    public int MAXOBJECTS = 10000;
    public int MAXVERTICES = 40000000;
    public int MAXMESHLETS = 1000000;
    public int MAXMESHLETUPDATESPERFRAME = 1568;
    public int MAXMESHLETSPERFRAME = MAXMESHLETS;
    public int GPUObjectData_SIZEOF = Float.BYTES * (16+4);
    public int VERTEX_SIZE = Float.BYTES * (4 + 4);
    public int MESHLETSIZE = (Float.BYTES * 4 * 3);
    public List<Frame> frames = new ArrayList<>();
    public List<Meshlet> meshlets = new ArrayList<>();
    public int RENDERCONFIGSIZE = (Integer.BYTES * 2) + (Float.BYTES * 1);
    public int RENDERSTATSOUTPUTSIZE = (Integer.BYTES * 3);
    int previousMeshletsDrawnCount = -1;
    public Map<Mesh.VERTATTRIB, List<Vector>> globalVertAttribs = new HashMap<>();
    public List<Mesh.VERTATTRIB> meshAttribsToLoad = new ArrayList<>(Arrays.asList(Mesh.VERTATTRIB.POSITION));
    public List<Mesh.VERTATTRIB> meshAttribsToRender = new ArrayList<>(Arrays.asList(Mesh.VERTATTRIB.POSITION, COLOR));
    public List<Integer> meshletVertexIndexBuffer = new ArrayList<>();
    public List<Integer> meshletLocalIndexBuffer = new ArrayList<>();
    public List<MeshletUpdateInfo> meshletsToBeUpdated = new ArrayList<>();
    public List<ObjectDataUpdate> objectInfoToBeUpdated = new ArrayList<>();
    public ArrayList<Integer> curFrameMeshletsDrawIndices = new ArrayList<>();
    public record ObjectDataUpdate(int index, Model model){}
    public record MeshletUpdateInfo(int index, Vector info, Vector bounds, Integer objectId, Float density, Meshlet meshlet){}

    public class Frame {
        public AllocatedBuffer cameraBuffer;
        public AllocatedBuffer objectBuffer;
        public long frameBuffer;
        public AllocatedBuffer vertexBuffer;
        public AllocatedBuffer meshletVertexBuffer;
        public AllocatedBuffer meshletVertexLocalIndexBuffer;
        public AllocatedBuffer renderConfigBuffer;
        public AllocatedBuffer renderOutputStatsBuffer;
        public AllocatedBuffer meshletDescBuffer;
        public AllocatedBuffer meshletsToBeDrawn;
        public AllocatedBuffer meshletsToBeRemovedBuffer;
        public AllocatedBuffer meshletsChildToBeAddedBuffer;
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

    @Override
    public void createLogicalDevice() {
        try(MemoryStack stack = stackPush()) {

            QueueFamilyIndices indices = VulkanUtilities.findQueueFamilies(physicalDevice, surface);

            queueFamilyIndices = indices;
            int[] uniqueQueueFamilies = indices.unique();

            VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.calloc(uniqueQueueFamilies.length, stack);

            for(int i = 0;i < uniqueQueueFamilies.length;i++) {
                VkDeviceQueueCreateInfo queueCreateInfo = queueCreateInfos.get(i);
                queueCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
                queueCreateInfo.queueFamilyIndex(uniqueQueueFamilies[i]);
                queueCreateInfo.pQueuePriorities(stack.floats(1.0f));
            }

            var deviceFeatures = VkPhysicalDeviceFeatures.calloc(stack);
            deviceFeatures.samplerAnisotropy(true);
            if(msaaEnabled) {
                deviceFeatures.sampleRateShading(true);
            }
            if(deviceFeatures.multiDrawIndirect()) {
                deviceFeatures.multiDrawIndirect(true);
            }

            var vkPhysicalDeviceVulkan11Features = VkPhysicalDeviceVulkan11Features.calloc(stack);
            vkPhysicalDeviceVulkan11Features.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_1_FEATURES);
            vkPhysicalDeviceVulkan11Features.shaderDrawParameters(true);
            vkPhysicalDeviceVulkan11Features.multiview(true);

            var physicalDevice12Features = VkPhysicalDeviceVulkan12Features.calloc(stack);
            physicalDevice12Features.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES);
            physicalDevice12Features.timelineSemaphore(true);
            physicalDevice12Features.descriptorIndexing(true);
            physicalDevice12Features.descriptorBindingPartiallyBound(true);
            physicalDevice12Features.runtimeDescriptorArray(true);

            var device13Features = VkPhysicalDeviceVulkan13Features.calloc(stack);
            device13Features.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_3_FEATURES);
            device13Features.dynamicRendering(true);

            createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
            createInfo.pQueueCreateInfos(queueCreateInfos);
            createInfo.pNext(vkPhysicalDeviceVulkan11Features);
            createInfo.pNext(physicalDevice12Features);
            createInfo.pNext(device13Features);
            // queueCreateInfoCount is automatically set
            createInfo.pEnabledFeatures(deviceFeatures);

            createInfo.ppEnabledExtensionNames(VulkanUtilities.asPointerBuffer(DEVICE_EXTENSIONS));
            if(ENABLE_VALIDATION_LAYERS) {
                createInfo.ppEnabledLayerNames(VulkanUtilities.asPointerBuffer(VALIDATION_LAYERS));
            }

            PointerBuffer pDevice = stack.pointers(VK_NULL_HANDLE);

            if(vkCreateDevice(physicalDevice, createInfo, null, pDevice) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create logical device");
            }

            VulkanUtilities.device = new VkDevice(pDevice.get(0), physicalDevice, createInfo);
            deletionQueue.add(() -> vkDestroyDevice(VulkanUtilities.device, null));

            PointerBuffer pQueue = stack.pointers(VK_NULL_HANDLE);

            vkGetDeviceQueue(VulkanUtilities.device, indices.graphicsFamily, 0, pQueue);
            graphicsQueue = new VkQueue(pQueue.get(0), VulkanUtilities.device);

            vkGetDeviceQueue(VulkanUtilities.device, indices.presentFamily, 0, pQueue);
            presentQueue = new VkQueue(pQueue.get(0), VulkanUtilities.device);

            vkGetDeviceQueue(VulkanUtilities.device, indices.computeFamily, 0, pQueue);
            computeQueue = new VkQueue(pQueue.get(0), VulkanUtilities.device);

            vkGetDeviceQueue(VulkanUtilities.device, indices.transferFamily, 0, pQueue);
            transferQueue = new VkQueue(pQueue.get(0), VulkanUtilities.device);
        }
    }

    @Override
    public void tick() {

        checkAndPerformBufferUpdates();
        updateMeshletDrawIndicesBuffer();

        if(previousMeshletsDrawnCount != curFrameMeshletsDrawIndices.size()) {
            recordCommandBuffers();
            previousMeshletsDrawnCount = curFrameMeshletsDrawIndices.size();
        }
    }

    public void updateMeshletDrawIndicesBuffer() {

        for (var frame : frames) {

            var bw = new BufferWriter(vmaAllocator, frame.meshletsToBeDrawn, Integer.BYTES, Integer.BYTES*MAXMESHLETSPERFRAME);
            bw.mapBuffer();
            for(int i = 0; i < curFrameMeshletsDrawIndices.size(); i++) {
//                bw.setPosition(i);
                bw.put(curFrameMeshletsDrawIndices.get(i));
            }
            bw.unmapBuffer();
        }
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

                int numTaskShadersToLaunch = (curFrameMeshletsDrawIndices.size() /32 + 1);

                log("num of task shader work groups launched: "+ numTaskShadersToLaunch);

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

                    vkCmdDrawMeshTasksEXT(commandBuffer, numTaskShadersToLaunch, 1, 1);
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

        if(curFrameMeshletsDrawIndices.size() > 0) {
            prepareFrame();
            drawFrame();
            submitFrame();

            var bufferReader = new BufferWriter(vmaAllocator, frames.get(currentDisplayBufferIndex).renderOutputStatsBuffer,
                    Integer.BYTES, RENDERSTATSOUTPUTSIZE);
            bufferReader.mapBuffer();
            var meshletRenderCount = bufferReader.buffer.getInt();
            bufferReader.setPosition(1);
            var meshletUpdateCount = bufferReader.buffer.getInt();
            bufferReader.unmapBuffer();

            if(meshletUpdateCount > 0) {
                bufferReader = new BufferWriter(vmaAllocator, frames.get(currentDisplayBufferIndex).meshletsToBeRemovedBuffer,
                        Integer.BYTES, Integer.BYTES * meshletUpdateCount);
                bufferReader.mapBuffer();

                var meshletsToBeUpdated = new ArrayList<Integer>();
                for (int i = 0; i < meshletUpdateCount; i++) {
                    bufferReader.setPosition(i);
                    int value = bufferReader.buffer.getInt();
                    meshletsToBeUpdated.add(value);
                    var removed = curFrameMeshletsDrawIndices.remove((Integer) value);
                    if (!removed) {
                        logPerSec("Couldnt remove " + value + " num of meshlets being rendered: " + curFrameMeshletsDrawIndices.size());
                    }
                }
                bufferReader.unmapBuffer();
            }

            logPerSec("Rendered meshlet count: " + meshletRenderCount + " update count: "+ meshletUpdateCount);
        }
        else {
            logPerSec("Currently not rendering anything");
        }
    }

    @Override
    public void swapChainRecreatedEvent() {
        for(var fb: frames) {
            vkDestroyFramebuffer(device, fb.frameBuffer, null);
        }

        vkDestroyImageView(device, depthAttachment.imageView, null);
        vmaDestroyImage(vmaAllocator, depthAttachment.allocatedImage.image, depthAttachment.allocatedImage.allocation);

        createDepthAttachment();
        initFrameBuffers();
        recordCommandBuffers();
    }

    public void addMeshlet(int ind, Meshlet meshlet) {

        var meshletUpdateInfo = new MeshletUpdateInfo(ind,
                new Vector(new float[]{meshlet.primitiveCount, meshlet.vertexCount, meshlet.indexBegin, meshlet.vertexBegin}),
                new Vector(new float[]{meshlet.pos.get(0), meshlet.pos.get(1), meshlet.pos.get(2), meshlet.boundRadius}),
                meshlet.objectId, meshlet.density, meshlet);

        meshletsToBeUpdated.add(meshletUpdateInfo);
        meshlets.add(meshlet);
    }

    public void addMeshlet(Meshlet meshlet) {

        var meshletUpdateInfo = new MeshletUpdateInfo(meshlets.size(),
                new Vector(new float[]{meshlet.primitiveCount, meshlet.vertexCount, meshlet.indexBegin, meshlet.vertexBegin}),
                new Vector(new float[]{meshlet.pos.get(0), meshlet.pos.get(1), meshlet.pos.get(2), meshlet.boundRadius}),
                meshlet.objectId, meshlet.density, meshlet);

        meshletsToBeUpdated.add(meshletUpdateInfo);
        meshlets.add(meshlet);
    }

    public void addModel(int ind, Model model) {
        var objectUpdateInfo = new ObjectDataUpdate(ind, model);
        models.remove(ind);
        models.add(ind, model);
        objectInfoToBeUpdated.add(objectUpdateInfo);
    }
    public void addModel(Model model) {
        var objectUpdateInfo = new ObjectDataUpdate(models.size(), model);
        models.add(model);
        objectInfoToBeUpdated.add(objectUpdateInfo);
    }


    public void createMeshlets(int vertsPerPrimitive, int maxVerts, int maxPrimitives) {

        for(int modelInd = 0; modelInd < models.size(); modelInd++) {
            log("model ind: "+modelInd);
            var model = models.get(modelInd);

            MeshletGen.MeshletGenOutput results = null;
            if(model instanceof PointCloud) {

                var root = genHierLODPointCloud(model.meshes.get(0), 64, 4);
                var meshletsInOrder = getMeshletsInBFOrder(root);

//                log("Num meshlets in hierarchy = "+ getNumMeshlets(root) + " num verts in hier: "+getNumVertsInHierarchy(root));

                var newVertsList = new HashMap<Mesh.VERTATTRIB, List<Vector>>();
                var keySetOrig = model.meshes.get(0).vertAttributes.keySet();

                for(var key: meshAttribsToLoad) {
                    if(!keySetOrig.contains(key)) {
                        throw new RuntimeException("Mesh "+ model.meshes.get(0).meshLocation + " does not have the required vertex attribute: "+ key);
                    }
                    newVertsList.put(key, new ArrayList<>());
                }

                int curVertOffset = globalVertAttribs.get(POSITION).size();
                var vertIndexList = new ArrayList<Integer>(64);
                var indexList = new ArrayList<Integer>(64);

                for(var meshlet: meshletsInOrder) {

                    meshlet.pos = new Vector(0,0,0);
                    var curBounds = new BoundValues();

                    for(int i = 0; i < meshlet.vertIndices.size(); i++) {
                        var vertInd = meshlet.vertIndices.get(i);
                        vertIndexList.add(curVertOffset);
                        indexList.add(i);
                        curVertOffset+=1;

                        for(var key: meshAttribsToLoad) {
                            globalVertAttribs.get(key).add(model.meshes.get(0).getAttributeList(key).get(vertInd));
                        }
                        var v = model.meshes.get(0).getAttributeList(POSITION).get(vertInd);
                        meshlet.pos = meshlet.pos.add(v);

                        if (v.get(0) > curBounds.maxx)
                            curBounds.maxx = v.get(0);
                        if (v.get(1) > curBounds.maxy)
                            curBounds.maxy = v.get(1);
                        if (v.get(2) > curBounds.maxz)
                            curBounds.maxz = v.get(2);

                        if (v.get(0) < curBounds.minx)
                            curBounds.minx = v.get(0);
                        if (v.get(1) < curBounds.miny)
                            curBounds.miny = v.get(1);
                        if (v.get(2) < curBounds.minz)
                            curBounds.minz = v.get(2);
                    }

                    meshlet.vertexCount = meshlet.vertIndices.size();
                    meshlet.vertexBegin = meshletVertexIndexBuffer.size();
                    meshlet.indexBegin = meshletLocalIndexBuffer.size();
                    meshlet.primitiveCount = meshlet.vertIndices.size();
                    meshlet.pos = meshlet.pos.scalarMul(1f/meshlet.vertexCount);
                    meshlet.boundRadius = calculateBoundRadius(curBounds);
                    meshlet.density = calculatePointCloudDensity(meshlet.boundRadius, meshlet.vertexCount);

                    meshlet.objectId = modelInd;
                    addMeshlet(meshlet);

                    meshletVertexIndexBuffer.addAll(vertIndexList);
                    meshletLocalIndexBuffer.addAll(indexList);
                    vertIndexList.clear();
                    indexList.clear();
                    meshlet.vertIndices = null;
                }
            }
            else {
                results = generateMeshlets(model.meshes.get(0), vertsPerPrimitive, maxVerts, maxPrimitives,
                        globalVertAttribs.get(Mesh.VERTATTRIB.POSITION).size(),
                        meshletVertexIndexBuffer.size(), meshletLocalIndexBuffer.size());

                for(var meshlet: results.meshlets()) {
                    meshlet.objectId = modelInd;
                    addMeshlet(meshlet);
                }

                for(var key: meshAttribsToLoad) {
                    if(!model.meshes.get(0).vertAttributes.containsKey(key)) {
                        throw new RuntimeException("Mesh "+ model.meshes.get(0).meshLocation + " does not have the required vertex attribute: "+ key);
                    }
                    globalVertAttribs.get(key).addAll(model.meshes.get(0).vertAttributes.get(key));
                }
                meshletVertexIndexBuffer.addAll(results.vertexIndexBuffer());
                meshletLocalIndexBuffer.addAll(results.localIndexBuffer());

            }

            log(" num of total verts: "+ globalVertAttribs.get(Mesh.VERTATTRIB.POSITION).size());
        }

    }

    @Override
    public void geometryUpdatedEvent() {
        updateVertexAndIndexBuffers();
    }

    public void checkAndPerformBufferUpdates() {

        if(previousMeshletsDrawnCount != curFrameMeshletsDrawIndices.size()) {
            for(var frame: frames) {
                var bw = new BufferWriter(vmaAllocator, frame.renderConfigBuffer, Integer.SIZE, RENDERCONFIGSIZE);
                bw.mapBuffer();
                bw.put(curFrameMeshletsDrawIndices.size());
                bw.put(MAXMESHLETUPDATESPERFRAME);
                bw.unmapBuffer();
            }
        }

        if(objectInfoToBeUpdated.size() > 0) {

            var alignmentSize = GPUObjectData_SIZEOF;
            int bufferSize = alignmentSize * models.size();

            for(var frame: frames) {
                var bw = new BufferWriter(vmaAllocator, frame.objectBuffer, alignmentSize, bufferSize);
                bw.mapBuffer();
                for(var data: objectInfoToBeUpdated) {
                    bw.setPosition(data.index);
                    bw.put(data.model.objectToWorldMatrix);
                    bw.putFloat(data.model.scale.getMax());
                    bw.putFloat(0);
                    bw.putFloat(0);
                    bw.putFloat(0);
                }
                bw.unmapBuffer();
            }
            objectInfoToBeUpdated.clear();
        }

        if(meshletsToBeUpdated.size() > 0) {

            for(var frame: frames) {
                var bw = new BufferWriter(vmaAllocator, frame.meshletDescBuffer, MESHLETSIZE, MESHLETSIZE * meshlets.size());
                bw.mapBuffer();

                for(var data: meshletsToBeUpdated) {
                    bw.setPosition(data.index);

                    if(data.info != null) {
                        bw.put(data.info);
                    }
                    else {
                        bw.buffer.position(data.index * MESHLETSIZE + Float.BYTES * 4);
                    }

                    if(data.bounds != null) {
                        bw.put(data.bounds);
                    }
                    else {
                        bw.buffer.position(data.index * MESHLETSIZE + Float.BYTES * 8);
                    }

                    if(data.objectId != null) {
                        bw.put((float)data.objectId);
                    }
                    else {
                        bw.put(-1.0f);
                    }

                    if(data.density != null) {
                        bw.put(data.density);
                    }

                }
                bw.unmapBuffer();
            }
            meshletsToBeUpdated.clear();
        }
    }

    @Override
    public void cameraUpdatedEvent() {
        updateCameraBuffer();
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

            var bw = new BufferWriter(vmaAllocator, globalVerticesStagingBuffer, VERTEX_SIZE, globalVertexBufferSize);
            bw.mapBuffer();

            for(int i = 0; i < globalVertAttribs.get(POSITION).size(); i++) {
                bw.setPosition(i);

                for(var vertAttrib: meshAttribsToRender){
                    if(vertAttrib == POSITION) {
                        bw.putFloat(globalVertAttribs.get(vertAttrib).get(i).get(0));
                        bw.putFloat(globalVertAttribs.get(vertAttrib).get(i).get(1));
                        bw.putFloat(globalVertAttribs.get(vertAttrib).get(i).get(2));
                        bw.putFloat(1f);
                    }

                    else if(vertAttrib == TEXTURE) {
                        bw.putFloat(globalVertAttribs.get(vertAttrib).get(i).get(0));
                        bw.putFloat(globalVertAttribs.get(vertAttrib).get(i).get(1));
                        bw.putFloat(0);
                        bw.putFloat(0);
                    }

                    else {
                        bw.put(globalVertAttribs.get(vertAttrib).get(i));
                    }

                }

                // Color - each meshlet will have a random colour
                // error condition
//                if(globalVertAttribs.get(COLOR).size() <= i) {
//                    byteBuffer.putFloat(1f);
//                    byteBuffer.putFloat(1f);
//                    byteBuffer.putFloat(1f);
//                    byteBuffer.putFloat(1f);
//                }
//                else {
//                    byteBuffer.putFloat(globalVertAttribs.get(COLOR).get(i).get(0));
//                    byteBuffer.putFloat(globalVertAttribs.get(COLOR).get(i).get(1));
//                    byteBuffer.putFloat(globalVertAttribs.get(COLOR).get(i).get(2));
//                    byteBuffer.putFloat(globalVertAttribs.get(COLOR).get(i).get(3));
//                }

            }
            bw.unmapBuffer();
//            vmaUnmapMemory(vmaAllocator, globalVerticesStagingBuffer.allocation);

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
                    .bindBuffer(3, new DescriptorBufferInfo(0, VK_WHOLE_SIZE, frame.renderOutputStatsBuffer.buffer),
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
                    .bindBuffer(3, new DescriptorBufferInfo(0, VK_WHOLE_SIZE, frame.renderConfigBuffer.buffer),
                            VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_MESH_BIT_EXT | VK_SHADER_STAGE_TASK_BIT_EXT)
                    .bindBuffer(4, new DescriptorBufferInfo(0, VK_WHOLE_SIZE, frame.meshletsToBeDrawn.buffer),
                            VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_MESH_BIT_EXT | VK_SHADER_STAGE_TASK_BIT_EXT)
                    .bindBuffer(5, new DescriptorBufferInfo(0, VK_WHOLE_SIZE, frame.meshletsToBeRemovedBuffer.buffer),
                            VK_DESCRIPTOR_TYPE_STORAGE_BUFFER,  VK_SHADER_STAGE_TASK_BIT_EXT)
                    .bindBuffer(6, new DescriptorBufferInfo(0, VK_WHOLE_SIZE, frame.meshletsChildToBeAddedBuffer.buffer),
                            VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_TASK_BIT_EXT)
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

            frame.meshletsToBeDrawn = createBufferVMA(
                    vmaAllocator,
                    Integer.BYTES * MAXMESHLETSPERFRAME,
                    VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
                    VMA_MEMORY_USAGE_AUTO,
                    VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT |
                            VMA_ALLOCATION_CREATE_HOST_ACCESS_ALLOW_TRANSFER_INSTEAD_BIT
            );

            frame.meshletsToBeRemovedBuffer = createBufferVMA(
                    vmaAllocator,
                    Integer.BYTES * MAXMESHLETUPDATESPERFRAME,
                    VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
                    VMA_MEMORY_USAGE_AUTO,
                    VMA_ALLOCATION_CREATE_HOST_ACCESS_RANDOM_BIT |
                            VMA_ALLOCATION_CREATE_MAPPED_BIT
            );
            frame.meshletsChildToBeAddedBuffer = createBufferVMA(
                    vmaAllocator,
                    Integer.BYTES * MAXMESHLETUPDATESPERFRAME,
                    VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
                    VMA_MEMORY_USAGE_AUTO,
                    VMA_ALLOCATION_CREATE_HOST_ACCESS_RANDOM_BIT |
                            VMA_ALLOCATION_CREATE_MAPPED_BIT
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

            frame.renderConfigBuffer = createBufferVMA(vmaAllocator, RENDERCONFIGSIZE,
                    VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, VMA_MEMORY_USAGE_AUTO,
                    VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT |
                            VMA_ALLOCATION_CREATE_HOST_ACCESS_ALLOW_TRANSFER_INSTEAD_BIT);

            frame.renderOutputStatsBuffer = createBufferVMA(
                    vmaAllocator,
                    RENDERSTATSOUTPUTSIZE,
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
                vmaDestroyBuffer(vmaAllocator, frame.renderOutputStatsBuffer.buffer, frame.renderOutputStatsBuffer.allocation);
                vmaDestroyBuffer(vmaAllocator, frame.renderConfigBuffer.buffer, frame.renderConfigBuffer.allocation);
                vmaDestroyBuffer(vmaAllocator, frame.meshletsToBeRemovedBuffer.buffer, frame.meshletsToBeRemovedBuffer.allocation);
                vmaDestroyBuffer(vmaAllocator, frame.meshletsChildToBeAddedBuffer.buffer, frame.meshletsChildToBeAddedBuffer.allocation);
                vmaDestroyBuffer(vmaAllocator, frame.meshletsToBeDrawn.buffer, frame.meshletsToBeDrawn.allocation);
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

            for(var f: frustumPlanes) {
                f.setValuesToBuffer(bw.buffer);
            }

            bw.unmapBuffer();
        }
    }

    @Override
    public void cleanUp() {
        // Wait for the device to complete all operations before release resources
        vkDeviceWaitIdle(device);

        cleanUpSwapChainAndSwapImages();
        frames.forEach(f -> vkDestroyFramebuffer(device, f.frameBuffer, null));

        vkDestroyImageView(device, depthAttachment.imageView, null);
        vmaDestroyImage(vmaAllocator, depthAttachment.allocatedImage.image, depthAttachment.allocatedImage.allocation);

        for(int i = deletionQueue.size()-1; i >= 0; i--) {
            deletionQueue.get(i).run();
        }
    }
}
