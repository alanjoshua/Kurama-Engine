package main;

import org.joml.Vector2fc;
import org.joml.Vector3fc;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import static org.lwjgl.vulkan.VK10.*;

public class Vertex {

    public static final int SIZEOF = (3 + 3 + 2 + 3) * Float.BYTES;
    public static final int OFFSETOF_POS = 0;
    public static final int OFFSETOF_COLOR = 3 * Float.BYTES;
    public static final int OFFSETOF_TEXCOORDS = 6 * Float.BYTES;
    public static final int OFFSETOF_NORMAL = 8 * Float.BYTES;

//    Vec3 pos;
//    Vec3 color;
//    Vec2 texCoord;
//    Vec3 normal

    public static VkVertexInputBindingDescription.Buffer getBindingDescription() {

        VkVertexInputBindingDescription.Buffer bindingDescription =
                VkVertexInputBindingDescription.calloc(1);

        bindingDescription.binding(0);
        bindingDescription.stride(Vertex.SIZEOF);
        bindingDescription.inputRate(VK_VERTEX_INPUT_RATE_VERTEX);

        return bindingDescription;
    }

    public static VkVertexInputAttributeDescription.Buffer getAttributeDescriptions() {

        var attributeDescriptions = VkVertexInputAttributeDescription.calloc(4);

        // Position
        VkVertexInputAttributeDescription posDescription = attributeDescriptions.get(0);
        posDescription.binding(0);
        posDescription.location(0);
        posDescription.format(VK_FORMAT_R32G32B32_SFLOAT);
        posDescription.offset(OFFSETOF_POS);

        // Color
        VkVertexInputAttributeDescription colorDescription = attributeDescriptions.get(1);
        colorDescription.binding(0);
        colorDescription.location(1);
        colorDescription.format(VK_FORMAT_R32G32B32_SFLOAT);
        colorDescription.offset(OFFSETOF_COLOR);

        // textCoords
        var texDescription = attributeDescriptions.get(2);
        texDescription.binding(0);
        texDescription.location(2);
        texDescription.format(VK_FORMAT_R32G32_SFLOAT);
        texDescription.offset(OFFSETOF_TEXCOORDS);

        // Normals
        VkVertexInputAttributeDescription normalDescription = attributeDescriptions.get(3);
        colorDescription.binding(0);
        colorDescription.location(3);
        colorDescription.format(VK_FORMAT_R32G32B32_SFLOAT);
        colorDescription.offset(OFFSETOF_NORMAL);

        return attributeDescriptions.rewind();
    }

}
