package main;

import org.joml.Vector2fc;
import org.joml.Vector3fc;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import static org.lwjgl.vulkan.VK10.*;

public class Vertex {

    public static final int SIZEOF = (3 + 2 + 3) * Float.BYTES;
    public static final int OFFSETOF_POS = 0;
    public static final int OFFSETOF_TEXCOORDS = 3 * Float.BYTES;
    public static final int OFFSETOF_NORMAL = 5 * Float.BYTES;

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

        var attributeDescriptions = VkVertexInputAttributeDescription.calloc(3);

        // Position
        VkVertexInputAttributeDescription posDescription = attributeDescriptions.get(0);
        posDescription.binding(0);
        posDescription.location(0);
        posDescription.format(VK_FORMAT_R32G32B32_SFLOAT);
        posDescription.offset(OFFSETOF_POS);

        // textCoords
        var texDescription = attributeDescriptions.get(1);
        texDescription.binding(0);
        texDescription.location(1);
        texDescription.format(VK_FORMAT_R32G32_SFLOAT);
        texDescription.offset(OFFSETOF_TEXCOORDS);

        // Normals
        VkVertexInputAttributeDescription normalDescription = attributeDescriptions.get(2);
        normalDescription.binding(0);
        normalDescription.location(2);
        normalDescription.format(VK_FORMAT_R32G32B32_SFLOAT);
        normalDescription.offset(OFFSETOF_NORMAL);

        return attributeDescriptions.rewind();
    }

}
