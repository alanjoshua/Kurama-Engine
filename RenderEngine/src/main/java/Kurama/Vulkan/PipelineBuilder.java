package Kurama.Vulkan;

import Kurama.Math.Vector;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static Kurama.Vulkan.ShaderSPIRVUtils.ShaderKind.*;
import static Kurama.Vulkan.ShaderSPIRVUtils.compileShaderFile;
import static Kurama.Vulkan.VulkanUtilities.*;
import static Kurama.utils.Logger.log;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class PipelineBuilder {

    public record ShaderStageCreateInfo(String shaderFile, int ShaderType, String entryPoint){
        public ShaderStageCreateInfo(String shaderFile, int ShaderType) {
            this(shaderFile, ShaderType, "main");
        }
    }
    public record VertexBindingDescription(int binding, int stride, int inputRate){}
    public record VertexAttributeDescription(int binding, int location, int format, int offset){}
    public record InputAssemblyCreateInfo(int topology, boolean primitiveRestartEnable){
        public InputAssemblyCreateInfo() {
            this(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST, false);
        }
    }
    public record ViewPort(float x, float y, float width, float height, float minDepth, float maxDepth){
        public ViewPort(float width, float height) {
            this(0f, 0f, width, height, 0f, 1f);
        }
    }
    public record Scissor(int offsetX, int offsetY, VkExtent2D extent){
        public Scissor(VkExtent2D extent) {
            this(0, 0, extent);
        }
    }
    public record PipelineRasterizationStateCreateInfo(boolean depthClampEnable, boolean rasterizerDiscardEnable,
                                                       int polygonMode, float lineWidth, int cullMode, int frontFace,
                                                       boolean depthBiasEnable, float depthBiasConstantFactor, float depthBiasClamp, float depthBiasSlopeFactor){

        public PipelineRasterizationStateCreateInfo() {
            this(false, false, VK_POLYGON_MODE_FILL, 1.0f,
                    VK_CULL_MODE_BACK_BIT, VK_FRONT_FACE_COUNTER_CLOCKWISE,
                    false, 0f, 0f, 0f);
        }

        public PipelineRasterizationStateCreateInfo(int cullMode, int frontFace) {
            this(false, false, VK_POLYGON_MODE_FILL, 1.0f,
                    cullMode, frontFace,
                    false, 0f, 0f, 0f);
        }
    }

    public record PipelineDepthStencilStateCreateInfo(boolean depthTestEnable, boolean depthWriteEnable,
                                                      int depthCompareOp, boolean depthBoundsTestEnable,
                                                      float minDepthBounds, float maxDepthBounds,
                                                      boolean stencilTestEnable) {
        public PipelineDepthStencilStateCreateInfo() {
            this(true, true, VK_COMPARE_OP_LESS_OR_EQUAL, false, 0f,1f,false);
        }

        public PipelineDepthStencilStateCreateInfo(boolean depthTestEnable, boolean depthWriteEnable, int depthCompareOp, boolean depthBoundsTestEnable, boolean stencilTestEnable) {
            this(depthTestEnable, depthWriteEnable, depthCompareOp, depthBoundsTestEnable, 0f, 1f, stencilTestEnable);
        }

    }

    public record PipelineColorBlendStateCreateInfo(boolean logicOpEnable, int logicOp, Vector blendConstants, int colorWriteMask, boolean blendEnable) {
        public PipelineColorBlendStateCreateInfo() {
            this(false, VK_LOGIC_OP_COPY, new Vector(new float[]{0,0,0,0}),
                    VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT,
                    false);
        }
    }
    public record PushConstant(int offset, int size, int stageFlags) {}
    public record PipelineMultisampleStateCreateInfo(
            boolean sampleShadingEnabled, int rasterizationSamples, float minSampleShading, boolean alphaToCoverageEnable, boolean alhphaToOneEnable) {

        public PipelineMultisampleStateCreateInfo() {
            this(false, 1, 1f, false, false);
        }

    }
    public record PipelineLayoutAndPipeline(long pipelineLayout, long pipeline){};

    public List<ShaderStageCreateInfo> shaderStages = new ArrayList<>();
    public VertexBindingDescription vertexBindingDescription;
    public List<VertexAttributeDescription> vertexAttributeDescriptions;
    public InputAssemblyCreateInfo inputAssemblyCreateInfo = new InputAssemblyCreateInfo();
    public ViewPort viewport;
    public Scissor scissor;
    public PipelineRasterizationStateCreateInfo rasterizer = new PipelineRasterizationStateCreateInfo();
    public PipelineDepthStencilStateCreateInfo depthStencil = new PipelineDepthStencilStateCreateInfo();
    public PipelineColorBlendStateCreateInfo colorBlendAttach = new PipelineColorBlendStateCreateInfo();
    public PipelineMultisampleStateCreateInfo multiSample;
    public PushConstant pushConstant;
    public long[] descriptorSetLayouts;
    public PipelineType pipelineType;
    public enum PipelineType {COMPUTE, VERTEX_FRAGMENT};

    public PipelineBuilder(PipelineType pipelineType) {
        super();
        this.pipelineType = pipelineType;
    }

    public PipelineBuilder() {
        this.pipelineType = PipelineType.VERTEX_FRAGMENT;
    }

    private PipelineLayoutAndPipeline buildVertexFragPipeline(VkDevice device, long renderPass) {
        if(viewport == null) {
            throw new IllegalArgumentException("Viewport cannot be null");
        }
        if(scissor == null) {
            throw new IllegalArgumentException("Scissor cannot be null");
        }

        try(var stack = stackPush()) {

            // SHADER STAGES
            var shaderStagesBuffer = VkPipelineShaderStageCreateInfo.calloc(shaderStages.size(), stack);
            var shaderModules = new ArrayList<Long>();
            var shaderSPIRVs = new ArrayList<ShaderSPIRVUtils.SPIRV>();

            for(int i = 0; i < shaderStages.size(); i++) {

                var shader = shaderStages.get(i);

                var entryPoint = stack.UTF8(shader.entryPoint);
                ShaderSPIRVUtils.ShaderKind shaderKind = null;

                switch (shader.ShaderType) {
                    case VK_SHADER_STAGE_VERTEX_BIT:
                        shaderKind = VERTEX_SHADER;
                        break;
                    case VK_SHADER_STAGE_FRAGMENT_BIT:
                        shaderKind = FRAGMENT_SHADER;
                        break;
                }

                if(shaderKind == null) {
                    throw new IllegalArgumentException("Invalid shader type was passed in");
                }
                var shaderSPRIV = compileShaderFile(shader.shaderFile, shaderKind);
                var shaderModule = createShaderModule(shaderSPRIV.bytecode(), device);
                shaderModules.add(shaderModule);
                shaderSPIRVs.add(shaderSPRIV);

                var shaderStageInfo = shaderStagesBuffer.get(i);
                shaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
                shaderStageInfo.stage(shader.ShaderType);
                shaderStageInfo.module(shaderModule);
                shaderStageInfo.pName(entryPoint);
            }

            // VERTEX INFO
            var vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc(stack);
            vertexInputInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);

            if(vertexBindingDescription == null) {
                vertexInputInfo.pVertexBindingDescriptions(null);
                vertexInputInfo.pVertexAttributeDescriptions(null);
            }
            else {
                if(vertexAttributeDescriptions == null || vertexAttributeDescriptions.size() == 0) {
                    throw new IllegalArgumentException("Vertex binding attributes cannot be null when binding description is present");
                }

                var bindingDescription =
                        VkVertexInputBindingDescription.calloc(1);

                bindingDescription.binding(vertexBindingDescription.binding);
                bindingDescription.stride(vertexBindingDescription.stride);
                bindingDescription.inputRate(vertexBindingDescription.inputRate);

                vertexInputInfo.pVertexBindingDescriptions(bindingDescription);

                var attributeDescriptions = VkVertexInputAttributeDescription.calloc(vertexAttributeDescriptions.size());
                for(int i = 0; i < vertexAttributeDescriptions.size(); i++) {
                    var attribInfo = vertexAttributeDescriptions.get(i);
                    var attrib = attributeDescriptions.get(i);
                    attrib.binding(attribInfo.binding);
                    attrib.location(attribInfo.location);
                    attrib.format(attribInfo.format);
                    attrib.offset(attribInfo.offset);
                }
                attributeDescriptions.rewind();
                vertexInputInfo.pVertexAttributeDescriptions(attributeDescriptions);
            }

            // ASSEMBLY STAGE
            var inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack);
            inputAssembly.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
            inputAssembly.topology(inputAssemblyCreateInfo.topology);
            inputAssembly.primitiveRestartEnable(inputAssemblyCreateInfo.primitiveRestartEnable);

            // VIEWPORT and SCISSOR
            VkViewport.Buffer viewportBuffer = VkViewport.calloc(1, stack);
            viewportBuffer.x(viewport.x);
            viewportBuffer.y(viewport.y);
            viewportBuffer.width(viewport.width);
            viewportBuffer.height(viewport.height);
            viewportBuffer.minDepth(viewport.minDepth);
            viewportBuffer.maxDepth(viewport.maxDepth);

            VkRect2D.Buffer scissorBuffer = VkRect2D.calloc(1, stack);
            scissorBuffer.offset(VkOffset2D.calloc(stack).set(scissor.offsetX, scissor.offsetY));
            scissorBuffer.extent(scissor.extent);

            VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack);
            viewportState.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);
            viewportState.pViewports(viewportBuffer);
            viewportState.pScissors(scissorBuffer);

            // ===> RASTERIZATION STAGE <===
            var rasterizerInfo = VkPipelineRasterizationStateCreateInfo.calloc(stack);
            rasterizerInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
            rasterizerInfo.depthClampEnable(rasterizer.depthClampEnable);
            rasterizerInfo.rasterizerDiscardEnable(rasterizer.rasterizerDiscardEnable);
            rasterizerInfo.polygonMode(rasterizer.polygonMode);
            rasterizerInfo.lineWidth(rasterizer.lineWidth);
            rasterizerInfo.cullMode(rasterizer.cullMode);
            rasterizerInfo.frontFace(rasterizer.frontFace);
            rasterizerInfo.depthBiasEnable(rasterizer.depthBiasEnable);

            var depthStencilInfo = VkPipelineDepthStencilStateCreateInfo.calloc(stack);
            depthStencilInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO);
            depthStencilInfo.depthTestEnable(depthStencil.depthTestEnable);
            depthStencilInfo.depthWriteEnable(depthStencil.depthWriteEnable);
            depthStencilInfo.depthCompareOp(depthStencil.depthCompareOp);
            depthStencilInfo.depthBoundsTestEnable(depthStencil.depthBoundsTestEnable);
            depthStencilInfo.minDepthBounds(depthStencil.minDepthBounds); // Optional
            depthStencilInfo.maxDepthBounds(depthStencil.maxDepthBounds); // Optional
            depthStencilInfo.stencilTestEnable(depthStencil.stencilTestEnable);

            // ===> MULTISAMPLING <===

            VkPipelineMultisampleStateCreateInfo multisampling = null;
            if(multiSample != null) {
                multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack);
                multisampling.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
                multisampling.sampleShadingEnable(multiSample.sampleShadingEnabled);
                multisampling.rasterizationSamples(multiSample.rasterizationSamples);
                multisampling.minSampleShading(multiSample.minSampleShading);
            }

            // ===> COLOR BLENDING <===
            var colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(1, stack);
            colorBlendAttachment.colorWriteMask(colorBlendAttach.colorWriteMask);
            colorBlendAttachment.blendEnable(colorBlendAttach.blendEnable);

            var colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack);
            colorBlending.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
            colorBlending.logicOpEnable(colorBlendAttach.logicOpEnable);
            colorBlending.logicOp(colorBlendAttach.logicOp);
            colorBlending.pAttachments(colorBlendAttachment);

            var blendConstants = colorBlendAttach.blendConstants;
            colorBlending.blendConstants(stack.floats(blendConstants.get(0), blendConstants.get(1), blendConstants.get(2), blendConstants.get(3)));

            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack);
            pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);

            if(descriptorSetLayouts != null) {
                pipelineLayoutInfo.pSetLayouts(stack.longs(descriptorSetLayouts));
            }

            // PUSH CONSTANTS
            if(pushConstant != null) {
                var pushConstantBuffer = VkPushConstantRange.calloc(1, stack);
                pushConstantBuffer.offset(pushConstant.offset);
                pushConstantBuffer.size(pushConstant.size);
                pushConstantBuffer.stageFlags(pushConstant.stageFlags);
                pipelineLayoutInfo.pPushConstantRanges(pushConstantBuffer);
            }

            // ===> PIPELINE LAYOUT CREATION <===
            LongBuffer pPipelineLayout = stack.longs(VK_NULL_HANDLE);

            if(vkCreatePipelineLayout(device, pipelineLayoutInfo, null, pPipelineLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create pipeline layout");
            }
            var pipelineLayout = pPipelineLayout.get(0);

            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack);
            pipelineInfo.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
            pipelineInfo.pStages(shaderStagesBuffer);
            pipelineInfo.pVertexInputState(vertexInputInfo);
            pipelineInfo.pInputAssemblyState(inputAssembly);
            pipelineInfo.pViewportState(viewportState);
            pipelineInfo.pRasterizationState(rasterizerInfo);
            pipelineInfo.pDepthStencilState(depthStencilInfo);
            pipelineInfo.pColorBlendState(colorBlending);
            pipelineInfo.layout(pipelineLayout);
            pipelineInfo.renderPass(renderPass);
            pipelineInfo.subpass(0);
            pipelineInfo.basePipelineHandle(VK_NULL_HANDLE);

            if(multiSample != null) {
                pipelineInfo.pMultisampleState(multisampling);
            }

            LongBuffer pGraphicsPipeline = stack.mallocLong(1);

            if(vkCreateGraphicsPipelines(device, VK_NULL_HANDLE, pipelineInfo, null, pGraphicsPipeline) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create graphics pipeline");
            }

            var graphicsPipeline = pGraphicsPipeline.get(0);

            // RELEASE RESOURCES
            shaderModules.forEach(s -> vkDestroyShaderModule(device, s, null));
            shaderSPIRVs.forEach(s -> s.free());

            return new PipelineLayoutAndPipeline(pipelineLayout, graphicsPipeline);
        }
    }

    public PipelineLayoutAndPipeline buildComputePipeline(VkDevice device) {

        if(descriptorSetLayouts == null) {
            throw new RuntimeException("Compute shaders must have descriptors");
        }

        try (var stack = stackPush()) {

            // SHADER STAGES
            var shaderStagesBuffer = VkPipelineShaderStageCreateInfo.calloc(shaderStages.size(), stack);
            var shaderModules = new ArrayList<Long>();
            var shaderSPIRVs = new ArrayList<ShaderSPIRVUtils.SPIRV>();

            for(int i = 0; i < shaderStages.size(); i++) {

                var shader = shaderStages.get(i);

                var entryPoint = stack.UTF8(shader.entryPoint);
                ShaderSPIRVUtils.ShaderKind shaderKind = null;

                switch (shader.ShaderType) {
                    case VK_SHADER_STAGE_VERTEX_BIT:
                        shaderKind = VERTEX_SHADER;
                        break;
                    case VK_SHADER_STAGE_FRAGMENT_BIT:
                        shaderKind = FRAGMENT_SHADER;
                        break;
                    case VK_SHADER_STAGE_COMPUTE_BIT:
                        shaderKind = COMPUTE_SHADER;
                        break;
                }

                if(shaderKind == null) {
                    throw new IllegalArgumentException("Invalid shader type was passed in");
                }
                var shaderSPRIV = compileShaderFile(shader.shaderFile, shaderKind);
                var shaderModule = createShaderModule(shaderSPRIV.bytecode(), device);
                shaderModules.add(shaderModule);
                shaderSPIRVs.add(shaderSPRIV);

                var shaderStageInfo = shaderStagesBuffer.get(i);
                shaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
                shaderStageInfo.stage(shader.ShaderType);
                shaderStageInfo.module(shaderModule);
                shaderStageInfo.pName(entryPoint);
            }

            var pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack);
            pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
            pipelineLayoutInfo.pSetLayouts(stack.longs(descriptorSetLayouts));

            // ===> PIPELINE LAYOUT CREATION <===
            LongBuffer pPipelineLayout = stack.longs(VK_NULL_HANDLE);
            vkCheck(vkCreatePipelineLayout(device, pipelineLayoutInfo, null, pPipelineLayout));

            // ===> PIPELINE CREATION <===
            LongBuffer pPipeline = stack.longs(VK_NULL_HANDLE);
            var pipelineInfo = VkComputePipelineCreateInfo.calloc(1, stack);
            pipelineInfo.sType(VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO);
            pipelineInfo.layout(pPipelineLayout.get(0));
            pipelineInfo.flags(0);
            pipelineInfo.stage(shaderStagesBuffer.get(0));
            
            vkCheck(vkCreateComputePipelines(device, VK_NULL_HANDLE, pipelineInfo, null, pPipeline));

            // RELEASE RESOURCES
            shaderModules.forEach(s -> vkDestroyShaderModule(device, s, null));
            shaderSPIRVs.forEach(s -> s.free());

            return new PipelineLayoutAndPipeline(pPipelineLayout.get(0), pPipeline.get(0));
        }
    }

    public PipelineLayoutAndPipeline build(VkDevice device, Long renderPass) {

        if(pipelineType == PipelineType.VERTEX_FRAGMENT) {
            return buildVertexFragPipeline(device, renderPass);
        }

        else {
            return buildComputePipeline(device);
        }

    }

}
