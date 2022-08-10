package Kurama.Vulkan;

import Kurama.Mesh.Texture;

import java.nio.ByteBuffer;

public class TextureVK extends Texture {

    public long id;

    public TextureVK(String fileName) {
    }

    public TextureVK(ByteBuffer buf) {
    }

    @Override
    public Long getId() {
        return id;
    }
}
