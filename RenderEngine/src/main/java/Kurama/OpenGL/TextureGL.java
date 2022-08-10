package Kurama.OpenGL;

import Kurama.Mesh.Texture;
import Kurama.buffers.RenderBuffer;
import Kurama.game.Game;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.*;

public class TextureGL extends Texture {

    public int id;

    public TextureGL(String fileName, int numRows, int numCols) {
        this(fileName);
        this.numRows = numRows;
        this.numCols = numCols;
    }

    public TextureGL(int textureId) {
        this.id = textureId;
        width = 0;
        height = 0;
    }

    public TextureGL(RenderBuffer buffer) {
        this.id = buffer.textureId;
        width = buffer.renderResolution.geti(0);
        height = buffer.renderResolution.geti(1);
    }

    public TextureGL(String fileName) {

        if(Game.GRAPHICS_API == Game.GraphicsApi.OPENGL) {
            ByteBuffer buff;

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                IntBuffer channels = stack.mallocInt(1);

                buff = stbi_load
                        (fileName, w, h, channels, 4);

                if (buff == null) {
                    throw new RuntimeException("Image file [" + fileName + "] not loaded: " + stbi_failure_reason());
                }

                width = w.get();
                height = h.get();
            }
            this.id = createTextureGL(buff);
        }
        else {
            width = 0;
            height = 0;
        }
        this.fileName = fileName;
    }

    public TextureGL(String fileName, float multiplier) {

        ByteBuffer buff;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            buff = stbi_load
                    (fileName, w, h, channels, 4);

            if (buff == null) {
                throw new RuntimeException("Image file [" + fileName + "] not loaded: " + stbi_failure_reason());
            }

            FloatBuffer floatBuf = buff.asFloatBuffer();
            floatBuf.rewind();
            for (int i = 0; i < floatBuf.remaining(); i++) {
                float curVal = floatBuf.get(i);
                floatBuf.put(i, curVal * multiplier);
            }
//            floatBuf.rewind();

            width = w.get();
            height = h.get();
        }
        this.id = createTextureGL(buff);

        this.fileName = fileName;
    }

    public TextureGL(ByteBuffer imageBuffer) {
        ByteBuffer buf;
        // Load Texture file
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            buf = stbi_load_from_memory(imageBuffer, w, h, channels, 4);
            if (buf == null) {
                throw new RuntimeException("Image file not loaded: " + stbi_failure_reason());
            }

            width = w.get();
            height = h.get();
        }

        this.id = createTextureGL(buf);

        stbi_image_free(buf);
    }

    public TextureGL(int width, int height, int pixelFormat) {
        this.id = glGenTextures();
        this.width = width;
        this.height = height;
        glBindTexture(GL_TEXTURE_2D,this.id);
        glTexImage2D(GL_TEXTURE_2D,0,GL_DEPTH_COMPONENT,this.width,this.height,0,pixelFormat,GL_FLOAT,(ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
        float borderColor[] = { 1.0f, 1.0f, 1.0f, 1.0f };
        glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, borderColor);
    }

    private int createTextureGL(ByteBuffer buf) {
        // Create a new OpenGL texture
        int textureId = glGenTextures();
        // Bind the texture
        glBindTexture(GL_TEXTURE_2D, textureId);

        // Tell OpenGL how to unpack the RGBA bytes. Each component is 1 byte size
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

        // Upload the texture data
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0,
                GL_RGBA, GL_UNSIGNED_BYTE, buf);
        // Generate Mip Map
        glGenerateMipmap(GL_TEXTURE_2D);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        return textureId;
    }

//    public void bind() {
//        glBindTexture(GL_TEXTURE_2D,id);
//    }

    public void cleanUp() {
        glDeleteTextures(id);
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public String toString() {
        return ""+id;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null)
            return false;

        if (getClass() != obj.getClass())
            return false;

        TextureGL t = (TextureGL)obj;
        return (id == t.id) && (width == t.width) && (height == t.height) && (fileName == t.fileName);
    }

    @Override
    public int hashCode() {
        return id;
    }
}
