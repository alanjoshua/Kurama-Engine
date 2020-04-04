package models.DataStructure;

import org.lwjgl.system.MemoryStack;

import java.lang.management.MemoryManagerMXBean;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.*;

//Class stolen from lwjgl gitbook

public class Texture {

    private final int id;

    public Texture(String fileName) throws Exception {
        this(loadTexture(fileName));
    }

    public Texture(int id) {
        this.id = id;
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D,id);
    }

    public void cleanUp() {
        glDeleteTextures(id);
    }

    public int getId() {
        return id;
    }

    public static int loadTexture(String fileName) throws Exception {
        int width;
        int height;
        ByteBuffer buff;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            buff = stbi_load(fileName,w,h,channels,4);
            if(buff == null) {
                throw new Exception("Image file [" + fileName + "] not loaded: " + stbi_failure_reason());
            }

            width = w.get();
            height = h.get();
        }

        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D,textureId);

        glPixelStorei(GL_UNPACK_ALIGNMENT,1);

        glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA,width,height,0,GL_RGBA,GL_UNSIGNED_BYTE,buff);

        glGenerateMipmap(GL_TEXTURE_2D);

        stbi_image_free(buff);

        return 0;
    }

}
