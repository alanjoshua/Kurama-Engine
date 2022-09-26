package Kurama.Vulkan;

import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;

import java.util.List;

public class PipelineBuilder {

    public static record ShaderStageCreateInfo(String shaderFile, int ShaderType, String entryPoint){}

    List<ShaderStageCreateInfo> shaderStages;
//    List

}
