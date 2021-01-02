package Kurama.shadow;

import Kurama.Mesh.Texture;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

public class ShadowMap {
    public static int DEFAULT_SHADOWMAP_WIDTH = 1024;
    public static int DEFAULT_SHADOWMAP_HEIGHT = 1024;

    public int shadowMapWidth = DEFAULT_SHADOWMAP_WIDTH;
    public int shadowMapHeight = DEFAULT_SHADOWMAP_HEIGHT;
    public final int depthMapFBO;
    public final Texture depthMap;

    public ShadowMap(int width, int height) {
        this.shadowMapWidth = width;
        this.shadowMapHeight = height;

        depthMapFBO = glGenFramebuffers();
        depthMap = new Texture(shadowMapWidth,shadowMapHeight,GL_DEPTH_COMPONENT);

        glBindFramebuffer(GL_FRAMEBUFFER, depthMapFBO);
        glFramebufferTexture2D(GL_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_TEXTURE_2D,depthMap.getId(),0);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        float borderColor[] = { 1.0f, 1.0f, 1.0f, 1.0f };
        glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, borderColor);

        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Could not create FrameBuffer");
        }
        glBindFramebuffer(GL_FRAMEBUFFER,0);
    }

    public void cleanUp() {
        glDeleteFramebuffers(depthMapFBO);
        depthMap.cleanUp();
    }

}
