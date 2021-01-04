package Kurama.buffers;

import Kurama.Math.Vector;
import Kurama.utils.Logger;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL30C.*;
import static org.lwjgl.opengl.GL32.glFramebufferTexture;

public class RenderBuffer {

    public final int fboId;
//    public final Texture texture;
    public final int textureId;
    public final int depthId;
    public Vector renderResolution;

    public RenderBuffer(Vector renderResolution) {

        this.renderResolution = renderResolution;
        fboId = glGenFramebuffers();
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, fboId);

        textureId = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB32F, (int)renderResolution.get(0), (int)renderResolution.get(1),
                0, GL_RGB, GL_FLOAT, (ByteBuffer) null);

        // For sampling
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        depthId = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, depthId);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, (int)renderResolution.get(0), (int)renderResolution.get(1));
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthId);

        glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, textureId, 0);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer intBuff = stack.mallocInt(1);
            intBuff.put(GL_COLOR_ATTACHMENT0);
            intBuff.flip();
            glDrawBuffers(intBuff);
        }

        if(glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            Logger.logError("error while creating render buffer. ");
            System.exit(1);
        }

        glBindFramebuffer(GL_FRAMEBUFFER,0);
    }

    public void resizeTexture(Vector renderResolution) {

        this.renderResolution = renderResolution;

        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB32F, (int)renderResolution.get(0), (int)renderResolution.get(1),
                0, GL_RGB, GL_FLOAT, (ByteBuffer) null);
        glBindTexture(GL_TEXTURE_2D, 0);

        glBindRenderbuffer(GL_RENDERBUFFER, depthId);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, (int)renderResolution.get(0), (int)renderResolution.get(1));
        glBindRenderbuffer(GL_RENDERBUFFER, 0);
    }

    public void cleanUp() {
        glDeleteFramebuffers(fboId);
        glDeleteTextures(textureId);
    }

}
