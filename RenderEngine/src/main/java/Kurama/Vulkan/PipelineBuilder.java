package Kurama.Vulkan;

import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;

import java.util.List;

public class PipelineBuilder {

    public record ShaderStageCreateInfo(String shaderFile, int ShaderType, String entryPoint){}
    public record VertexBindingDescription(int binding, int stride, int inputRate){}
    public record VertexAttributeDescription(int binding, int location, int format, int offset){}
    public record VertexInputStateCreateInfo(VertexBindingDescription vertexBindingDescription, List<VertexAttributeDescription> vertexAttributeDescription){}
    public record InputAssemblyCreateInfo(int topology, boolean primitiveRestartEnable){}

    List<ShaderStageCreateInfo> shaderStages;
    VertexInputStateCreateInfo vertexInputStateCreateInfo;
    InputAssemblyCreateInfo inputAssemblyCreateInfo;


}
