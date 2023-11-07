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
    public int MAXOBJECTS = 1000;
    public int MAXVERTICES = 1280000;
    public int MAXMESHLETS = 20000;
    public int MAXMESHLETUPDATESPERFRAME = 10000;
    public int MAXMESHLETSPERFRAME = MAXMESHLETS;
    public int GPUObjectData_SIZEOF = Float.BYTES * (16+4);
    public int VERTEX_SIZE = Float.BYTES * (4 + 4);
    public int MESHLETSIZE = (Float.BYTES * 6) + (Integer.BYTES * 6);
    public List<Frame> frames = new ArrayList<>();
    public List<Meshlet> meshlets = new ArrayList<>();
    public HashMap<Meshlet, Integer> meshletToIndexMapping = new HashMap<>();
    public int RENDERCONFIGSIZE = (Integer.BYTES * 5) + (Float.BYTES * 1);
    public int RENDERSTATSOUTPUTSIZE = (Integer.BYTES * 4);
    int previousMeshletsDrawnCount = -1;
    public float desiredDensityThreshold = 2000;
    public int numTreeDepthLevelsToRender = 3;
    public boolean individualDepthLevelToggle = true;
    public boolean updateNumTreeDepthLevelsToRender = false;
    public Map<Mesh.VERTATTRIB, List<Vector>> globalVertAttribs = new HashMap<>();
    public List<Mesh.VERTATTRIB> meshAttribsToLoad = new ArrayList<>(Arrays.asList(Mesh.VERTATTRIB.POSITION));
    public List<Mesh.VERTATTRIB> meshAttribsToRender = new ArrayList<>(Arrays.asList(Mesh.VERTATTRIB.POSITION, COLOR));
    public List<MeshletUpdateInfo> meshletsToBeUpdated = new ArrayList<>();
    public List<ObjectDataUpdate> objectInfoToBeUpdated = new ArrayList<>();
    public ArrayList<Integer> curFrameMeshletsDrawIndices = new ArrayList<>();
    public record ObjectDataUpdate(int index, Model model){}
    public record MeshletUpdateInfo(int index, Integer vertexBegin, Integer vertexCount, Vector bounds,
                                    Integer objectId, Float density, Integer treeDepth,
                                    Boolean childrenRendered, Integer parentId, Float cumDensity,
                                    Meshlet meshlet){}

    public class Frame {
        public AllocatedBuffer cameraBuffer;
        public AllocatedBuffer objectBuffer;
        public long frameBuffer;
        public AllocatedBuffer vertexBuffer;
        public AllocatedBuffer renderConfigBuffer;
        public AllocatedBuffer renderOutputStatsBuffer;
        public AllocatedBuffer meshletDescBuffer;
        public AllocatedBuffer meshletsToBeDrawn;
        public AllocatedBuffer meshletsToBeRemovedBuffer;
        public AllocatedBuffer meshletsChildrenToBeRemovedBuffer;
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

        VK10.vkWaitForFences(VulkanUtilities.device, drawFences.get(currentDisplayBufferIndex), true, VulkanUtilities.UINT64_MAX);
        VK10.vkResetFences(VulkanUtilities.device, drawFences.get(currentDisplayBufferIndex));

        checkAndPerformBufferUpdates();
        updateMeshletDrawIndicesBuffer();

        if(previousMeshletsDrawnCount != curFrameMeshletsDrawIndices.size()) {
            recordCommandBuffers();
            previousMeshletsDrawnCount = curFrameMeshletsDrawIndices.size();
        }
    }

    public void updateMeshletDrawIndicesBuffer() {

//        for (var frame : frames) {

            var bw = new BufferWriter(vmaAllocator, frames.get(currentDisplayBufferIndex).meshletsToBeDrawn, Integer.BYTES, Integer.BYTES*MAXMESHLETSPERFRAME);
            bw.mapBuffer();
            bw.setPosition(0);
            for(int i = 0; i < curFrameMeshletsDrawIndices.size(); i++) {
                bw.setPosition(i);
                bw.put(curFrameMeshletsDrawIndices.get(i));
            }
            bw.unmapBuffer();
//        }
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

                log("num of task shader work groups launched: "+ numTaskShadersToLaunch + " cur fps: "+game.displayFPS);

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
    public void drawFrame() {
        try(MemoryStack stack = stackPush()) {

            // Submit rendering commands to GPU
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.waitSemaphoreCount(1);

            submitInfo.pWaitSemaphores(stack.longs(presentCompleteSemaphore));
            submitInfo.pSignalSemaphores(stack.longs(renderCompleteSemaphore));
            submitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));
            submitInfo.pCommandBuffers(stack.pointers(drawCmds.get(currentDisplayBufferIndex)));

            int vkResult;
            if((vkResult = vkQueueSubmit(graphicsQueue, submitInfo, drawFences.get(currentDisplayBufferIndex))) != VK_SUCCESS) {
                VK10.vkResetFences(VulkanUtilities.device, drawFences.get(currentDisplayBufferIndex));
                throw new RuntimeException("Failed to submit draw command buffer: " + vkResult);
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
            var meshletRemoveCount = bufferReader.buffer.getInt();
            bufferReader.setPosition(2);
            var meshletChildAddedCount = bufferReader.buffer.getInt();
            bufferReader.setPosition(3);
            var meshletChildrenToBeRemovedCount = bufferReader.buffer.getInt();

            bufferReader.unmapBuffer();

            if(meshletRemoveCount > 0) {
                bufferReader = new BufferWriter(vmaAllocator, frames.get(currentDisplayBufferIndex).meshletsToBeRemovedBuffer,
                        Integer.BYTES, Integer.BYTES * meshletRemoveCount);
                bufferReader.mapBuffer();

                var meshletsBeingRemoved = new ArrayList<Integer>();
                for (int i = 0; i < meshletRemoveCount; i++) {
//                    bufferReader.setPosition(i);
                    int index = bufferReader.buffer.getInt();

                    // Don't remove ROOT node
                    if(index == 0) {
                        continue;
                    }

                    meshletsBeingRemoved.add(index);

                    var removed = curFrameMeshletsDrawIndices.remove((Integer) index);
                    if (!removed) {
                        logPerSec("Couldnt remove " + index + " num of meshlets being rendered: " + curFrameMeshletsDrawIndices.size());
                    }
                    // If it was removed successfully, update gpu meshlet info
                    else {
//                        var meshletUpdateInfo = new MeshletUpdateInfo(index, null,
//                                null, null, null, null, null,
//                                false, false, null, null);
//                        meshletsToBeUpdated.add(meshletUpdateInfo);
                    }
                }
                logPerSec("Mehslets being removed = " + meshletsBeingRemoved);
                bufferReader.unmapBuffer();
//                logPerSec("Num of 0s present: " + meshletsToBeUpdated.stream().filter(i -> i == 0).count() + " out of total=" + meshletsToBeUpdated);
            }

            if(meshletChildAddedCount > 0) {

                bufferReader = new BufferWriter(vmaAllocator, frames.get(currentDisplayBufferIndex).meshletsChildToBeAddedBuffer,
                        Integer.BYTES, Integer.BYTES * meshletChildAddedCount);
                bufferReader.mapBuffer();

//                double averageVal = 0;
                var meshletsBeingAdded = new ArrayList<Integer>();
                for (int i = 0; i < meshletChildAddedCount; i++) {
                    var parentId = bufferReader.buffer.getInt();

                    if(parentId < 0 || parentId >= meshlets.size()) continue;

//                    var meshletUpdateInfo = new MeshletUpdateInfo(parentId, null,
//                            null, null, null, null, null,
//                            true, true, null, null);
//                    meshletsToBeUpdated.add(meshletUpdateInfo);

                    for(var child: meshlets.get(parentId).children) {
                        var childInd = meshletToIndexMapping.get(child);

                        if(childInd < 0 || childInd >= meshlets.size()) {
                            throw new RuntimeException("Child ind is out of bounds: "+ childInd);
                        }

                        if(!curFrameMeshletsDrawIndices.contains(childInd)) {
                            curFrameMeshletsDrawIndices.add(childInd);

//                            var meshletUpdateInfo = new MeshletUpdateInfo(childInd, null,
//                                    null, null, null, null, null,
//                                    false, null, null, null);
//                            meshletsToBeUpdated.add(meshletUpdateInfo);
                        }
                        meshletsBeingAdded.add(childInd);
                    }
//                    averageVal += value;
//                    densityVals.add(value);
                }
//                averageVal /= meshletChildAddedCount;
                logPerSec("children being added: " + meshletsBeingAdded);
                bufferReader.unmapBuffer();
            }

            if(meshletChildrenToBeRemovedCount > 0) {
                bufferReader = new BufferWriter(vmaAllocator, frames.get(currentDisplayBufferIndex).meshletsChildrenToBeRemovedBuffer,
                        Integer.BYTES, Integer.BYTES * meshletChildrenToBeRemovedCount);
                bufferReader.mapBuffer();

                var childrenAlreadyRemoved = new HashSet<Meshlet>();

                for (int i = 0; i < meshletChildrenToBeRemovedCount; i++) {
                    var parentId = bufferReader.buffer.getInt();

                    if(parentId < 0 || parentId >= meshlets.size()) continue;

//                    var meshletUpdateInfo = new MeshletUpdateInfo(parentId, null,
//                            null, null, null, null, null,
//                            true, false, null, null);
//                    meshletsToBeUpdated.add(meshletUpdateInfo);

//                    log("Removing children subtree");
                    var subTree = getMeshletsInBFOrder(meshlets.get(parentId));
                    subTree.remove(0); // do not include itself

                    for(var child: subTree) {
                        if(childrenAlreadyRemoved.contains(child)) {
                            continue;
                        }
                        childrenAlreadyRemoved.add(child);

                        var childInd = meshletToIndexMapping.get(child);

                        if(childInd < 0 || childInd >= meshlets.size()) {
                            logPerSec("Child ind is out of bounds: "+ childInd);
                            continue;
                        }

                        var removed = curFrameMeshletsDrawIndices.remove(childInd);
                        if (!removed) {
//                            logPerSec("Couldnt remove child=" + childInd);
                        }
                        else {
                            var meshletUpdateInfo = new MeshletUpdateInfo(childInd, null,
                                    null, null, null, null, null,
                                    false, null, null, null);
                            meshletsToBeUpdated.add(meshletUpdateInfo);
                        }

                    }

                }

                bufferReader.unmapBuffer();
            }

            logPerSec("Rendered meshlet count: " + meshletRenderCount + " num task shaders: " + (curFrameMeshletsDrawIndices.size() /32 + 1) +  " direct remove count: "+ meshletRemoveCount + " add children count: " + meshletChildAddedCount + " children removed count: " + meshletChildrenToBeRemovedCount);

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

    public void addMeshlet(Meshlet meshlet) {

        var parentInd = meshletToIndexMapping.get(meshlet.parent);
        if(parentInd == null) {
            parentInd = 0;
        }

        var meshletUpdateInfo = new MeshletUpdateInfo(meshlets.size(), meshlet.vertexBegin, meshlet.vertexCount,
                new Vector(new float[]{meshlet.pos.get(0), meshlet.pos.get(1), meshlet.pos.get(2), meshlet.boundRadius}),
                meshlet.objectId, meshlet.density, meshlet.treeDepth, false,
                parentInd, 0f, meshlet);

        meshletsToBeUpdated.add(meshletUpdateInfo);

        meshletToIndexMapping.put(meshlet, meshlets.size());
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


    public void createMeshletsAndSyncGeoData(int maxVerts) {

        for(int modelInd = 0; modelInd < models.size(); modelInd++) {
            log("model ind: "+modelInd);
            var model = models.get(modelInd);

            if(model instanceof PointCloud && ((PointCloud)model).root != null) continue;

            for(var key: meshAttribsToLoad) {
                if(!model.meshes.get(0).vertAttributes.containsKey(key)) {
                    throw new RuntimeException("Mesh "+ model.meshes.get(0).meshLocation + " does not have the required vertex attribute: "+ key);
                }
            }

            MeshletGen.MeshletGenOutput results;
            if(model instanceof PointCloud) {
                PointCloud pointCloud = (PointCloud) model;

                // Must create the meshlet structures
                if(pointCloud.root == null) {

                    var root = genHierLODPointCloud(model.meshes.get(0), 64, 4);
                    var meshletsInOrder = getMeshletsInBFOrder(root);

                    int vertexIndexOffset = globalVertAttribs.get(POSITION).size();
                    float avgNumVerts = 0, averageDensity = 0, averageRadius = 0, minD = Float.POSITIVE_INFINITY, maxD = Float.NEGATIVE_INFINITY, minR = Float.POSITIVE_INFINITY, maxR = Float.NEGATIVE_INFINITY;

                    for (var meshlet : meshletsInOrder) {
                        meshlet.vertexBegin = vertexIndexOffset;
                        meshlet.pos = new Vector(0, 0, 0);
                        meshlet.vertexCount = meshlet.vertIndices.size();
                        avgNumVerts += meshlet.vertexCount;
                        var curBounds = new BoundValues();

                        for (int i = 0; i < meshlet.vertIndices.size(); i++) {
                            var vertInd = meshlet.vertIndices.get(i);

                            for (var key : meshAttribsToLoad) {
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

                        meshlet.pos = meshlet.pos.scalarMul(1f / meshlet.vertexCount);
                        meshlet.boundRadius = calculateBoundRadius(curBounds);
                        meshlet.density = calculatePointCloudDensity(meshlet.boundRadius, meshlet.vertexCount);

                        averageDensity += meshlet.density;
                        averageRadius += meshlet.boundRadius;

                        if (meshlet.boundRadius < minR) {
                            minR = meshlet.boundRadius;
                        }
                        if (meshlet.boundRadius > maxR) {
                            maxR = meshlet.boundRadius;
                        }
                        if (meshlet.density < minD) {
                            minD = meshlet.density;
                        }
                        if (meshlet.density > maxD) {
                            maxD = meshlet.density;
                        }

                        meshlet.objectId = modelInd;

                        meshlet.vertIndices = null;
                        vertexIndexOffset += meshlet.vertexCount;
                        addMeshlet(meshlet);
                    }

                    ((PointCloud) model).root = root;
                }

                // Point cloud model already contains the meshlet information, which means it has already been directly added to the renderer
                else {
//                    if(pointCloud.maxVertsPerMeshlet > maxVerts) {
//                        throw new IllegalArgumentException("The Point Cloud "+pointCloud.identifier + " max vert param is greater than the Renderer's value of " + maxVerts);
//                    }

//                    getMeshletsInBFOrder(pointCloud.root).forEach(m -> addMeshlet(m));
                }
            }
            else {

                results = generateMeshlets(model.meshes.get(0), maxVerts);

                var vertIndexOffset = globalVertAttribs.get(POSITION).size();
                for(var meshlet: results.meshlets()) {
                    meshlet.objectId = modelInd;
                    meshlet.vertexBegin += vertIndexOffset;
                    addMeshlet(meshlet);
                }

                for(var vInd: results.vertexIndexBuffer()) {
                    for(var key: meshAttribsToLoad) {
                        globalVertAttribs.get(key).add(model.meshes.get(0).vertAttributes.get(key).get(vInd));
                    }
                }

            }

            log(" num of total verts: "+ globalVertAttribs.get(Mesh.VERTATTRIB.POSITION).size());

            // Clear data from CPU RAM to save memory
            // TODO: Use flag to denote whether or not to delete from RAM
            for(var mesh: model.meshes) {
                mesh.vertAttributes = null;
                mesh.indices = null;
            }
        }

    }

    @Override
    public void geometryUpdatedEvent() {
        updateVertexAndIndexBuffers();
    }

    public void checkAndPerformBufferUpdates() {

        if(previousMeshletsDrawnCount != curFrameMeshletsDrawIndices.size()) {
            for(var frame: frames) {
                var bw = new BufferWriter(vmaAllocator, frame.renderConfigBuffer, RENDERCONFIGSIZE, RENDERCONFIGSIZE);
                bw.mapBuffer();
                bw.put(curFrameMeshletsDrawIndices.size());
                bw.put(MAXMESHLETUPDATESPERFRAME);
                bw.put(numTreeDepthLevelsToRender);
                bw.put(individualDepthLevelToggle?1: 0);
                bw.put(desiredDensityThreshold);
                bw.put(globalVertAttribs.get(POSITION).size());
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

                    if(data.vertexBegin != null) {
                        bw.put(data.vertexBegin);
                    }
                    else {
//                        bw.put(0f);
                    }
                    if(data.vertexCount != null) {
                        bw.buffer.position((bw.alignmentSize * data.index) + (1 * Integer.BYTES));
                        bw.put(data.vertexCount);
                    }
                    else {
//                        bw.put(0f);
                    }

                    if(data.objectId != null) {
                        bw.buffer.position((bw.alignmentSize * data.index) + (2 * Integer.BYTES));
                        bw.put(data.objectId);
                    }
                    else {
//                        bw.put(-1.0f);
                    }

                    if(data.density != null) {
                        bw.buffer.position((bw.alignmentSize * data.index) + (3 * Integer.BYTES));
                        bw.put(data.density);
                    }
                    else {
//                        bw.put(-1f);
                    }

                    if(data.bounds != null) {
                        bw.buffer.position((bw.alignmentSize * data.index) + (3 * Integer.BYTES) + (1 * Float.BYTES));
                        bw.put(data.bounds);
                    }
                    else {
//                        bw.put(0f);bw.put(0f);bw.put(0f);bw.put(0f);
                    }

                    if(data.treeDepth != null) {
                        bw.buffer.position((bw.alignmentSize * data.index) + (3 * Integer.BYTES) + (5 * Float.BYTES));
                        bw.put(data.treeDepth);
                    }
                    else {
//                        bw.put(-1);

                    }

                    if(data.cumDensity != null) {
                        bw.buffer.position((bw.alignmentSize * data.index) + (4 * Integer.BYTES) + (5 * Float.BYTES));
                        bw.put(data.cumDensity);
                    }

                    if(data.childrenRendered != null) {
                        bw.buffer.position((bw.alignmentSize * data.index) + (4 * Integer.BYTES) + (6 * Float.BYTES));
                        bw.put(data.childrenRendered);
                    }

                    if(data.parentId != null) {
                        bw.buffer.position((bw.alignmentSize * data.index) + (5 * Integer.BYTES) + (6 * Float.BYTES));
                        bw.put(data.parentId);
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

            }
            bw.unmapBuffer();

            var vertexCopy = VkBufferCopy.calloc(1, stack);

            for(var frame: frames) {

                Consumer<VkCommandBuffer> copyCmd = cmd -> {

                    vertexCopy.dstOffset(0);
                    vertexCopy.size(globalVertexBufferSize);
                    vertexCopy.srcOffset(0);
                    vkCmdCopyBuffer(cmd, globalVerticesStagingBuffer.buffer, frame.vertexBuffer.buffer, vertexCopy);
                };

                VulkanUtilities.submitImmediateCommand(copyCmd, singleTimeTransferCommandContext);
            }

        vmaDestroyBuffer(vmaAllocator, globalVerticesStagingBuffer.buffer, globalVerticesStagingBuffer.allocation);
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
                    .bindBuffer(1, new DescriptorBufferInfo(0, VK_WHOLE_SIZE, frame.renderConfigBuffer.buffer),
                            VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_MESH_BIT_EXT | VK_SHADER_STAGE_TASK_BIT_EXT)
                    .bindBuffer(2, new DescriptorBufferInfo(0, VK_WHOLE_SIZE, frame.meshletsToBeDrawn.buffer),
                            VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_MESH_BIT_EXT | VK_SHADER_STAGE_TASK_BIT_EXT)
                    .bindBuffer(3, new DescriptorBufferInfo(0, VK_WHOLE_SIZE, frame.meshletsToBeRemovedBuffer.buffer),
                            VK_DESCRIPTOR_TYPE_STORAGE_BUFFER,  VK_SHADER_STAGE_TASK_BIT_EXT)
                    .bindBuffer(4, new DescriptorBufferInfo(0, VK_WHOLE_SIZE, frame.meshletsChildToBeAddedBuffer.buffer),
                            VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_TASK_BIT_EXT)
                    .bindBuffer(5, new DescriptorBufferInfo(0, VK_WHOLE_SIZE, frame.meshletsChildrenToBeRemovedBuffer.buffer),
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
            frame.meshletsChildrenToBeRemovedBuffer = createBufferVMA(
                    vmaAllocator,
                    Integer.BYTES * MAXMESHLETUPDATESPERFRAME,
                    VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
                    VMA_MEMORY_USAGE_AUTO,
                    VMA_ALLOCATION_CREATE_HOST_ACCESS_RANDOM_BIT |
                            VMA_ALLOCATION_CREATE_MAPPED_BIT
            );

            // Initialize with negative values
            var bw = new BufferWriter(vmaAllocator, frame.meshletsToBeRemovedBuffer, Integer.BYTES, Integer.BYTES*MAXMESHLETSPERFRAME);
            bw.mapBuffer();
            for(int i = 0; i < MAXMESHLETSPERFRAME; i++) {
                bw.put(-1);
            }
            bw.unmapBuffer();

            frame.meshletsChildToBeAddedBuffer = createBufferVMA(
                    vmaAllocator,
                    Integer.BYTES * MAXMESHLETUPDATESPERFRAME,
                    VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
                    VMA_MEMORY_USAGE_AUTO,
                    VMA_ALLOCATION_CREATE_HOST_ACCESS_RANDOM_BIT |
                            VMA_ALLOCATION_CREATE_MAPPED_BIT
            );

            // Initialize with -1 values
            bw = new BufferWriter(vmaAllocator, frame.meshletsChildToBeAddedBuffer, Integer.BYTES, Integer.BYTES*MAXMESHLETSPERFRAME);
            bw.mapBuffer();
            for(int i = 0; i < MAXMESHLETSPERFRAME; i++) {
                bw.put(-1);
            }
            bw.unmapBuffer();

            frame.vertexBuffer = VulkanUtilities.createBufferVMA(vmaAllocator,
                    MAXVERTICES * VERTEX_SIZE,
                    VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
                    VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE, VMA_ALLOCATION_CREATE_DEDICATED_MEMORY_BIT
            );

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
                vmaDestroyBuffer(vmaAllocator, frame.renderOutputStatsBuffer.buffer, frame.renderOutputStatsBuffer.allocation);
                vmaDestroyBuffer(vmaAllocator, frame.renderConfigBuffer.buffer, frame.renderConfigBuffer.allocation);
                vmaDestroyBuffer(vmaAllocator, frame.meshletsToBeRemovedBuffer.buffer, frame.meshletsToBeRemovedBuffer.allocation);
                vmaDestroyBuffer(vmaAllocator, frame.meshletsChildrenToBeRemovedBuffer.buffer, frame.meshletsChildrenToBeRemovedBuffer.allocation);
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
        // GLSL camera struct layout
        //layout (set = 0, binding = 0) uniform CameraBuffer
//    {
//        mat4 projview;
//        mat4 view;
//        mat4 proj;
//        vec4 frustumPlanes[6];
//    } camera;

        var alignment = Float.BYTES * ((3 * 16) + (6 * 4));
        var bufferSize = alignment;

        var perspectiveProjectionMatrix = controller.mainCamera.getPerspectiveProjectionMatrix();
        perspectiveProjectionMatrix.getData()[1][1] *= -1;
        var camViewMat = controller.mainCamera.worldToObject;
        var projView = perspectiveProjectionMatrix.matMul(camViewMat);

        var frustumPlanes = Arrays.asList(((PointCloudController)game).mainCamera.frustumIntersection.planes);

        for(var frame: frames) {
            var bw = new BufferWriter(vmaAllocator, frame.cameraBuffer, alignment, bufferSize);
            bw.mapBuffer();

            projView.setValuesToBuffer(bw.buffer);
            camViewMat.setValuesToBuffer(bw.buffer);
            perspectiveProjectionMatrix.setValuesToBuffer(bw.buffer);

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
