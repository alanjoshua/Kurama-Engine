package Kurama.Vulkan;

import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;

import javax.swing.text.View;
import java.util.List;

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

    List<ShaderStageCreateInfo> shaderStages;
    VertexBindingDescription vertexBindingDescription;
    List<VertexAttributeDescription> vertexAttributeDescriptions;
    InputAssemblyCreateInfo inputAssemblyCreateInfo;
    ViewPort viewport;
    Scissor scissor;

}
