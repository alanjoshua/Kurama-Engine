package Kurama.renderingEngine.defaultRenderPipeline;

import Kurama.Mesh.Mesh;
import Kurama.geometry.MeshBuilder;
import Kurama.renderingEngine.*;
import Kurama.shader.ShaderProgram;

import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.glBindTexture;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

public class FullScreenQuadBlock extends RenderBlock {

    private static String quadShaderID = "particleShader";
    private ShaderProgram quadShader;
    private Mesh quad;

    public FullScreenQuadBlock(String id, RenderPipeline pipeline) {
        super(id, pipeline);
    }

    @Override
    public void setup(RenderBlockInput input) {
        quadShader = new ShaderProgram(quadShaderID);

        try {
            quadShader.createVertexShader("src/main/java/Kurama/renderingEngine/defaultRenderPipeline/shaders/fullscreenQuad_Vertex.glsl");
            quadShader.createFragmentShader("src/main/java/Kurama/renderingEngine/defaultRenderPipeline/shaders/fullscreenQuad_Fragment.glsl");
            quadShader.link();

            quadShader.createUniform("texture_sampler");
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        quad = MeshBuilder.buildFullscreenQuad();
        renderPipeline.initializeMesh(quad);
    }

    @Override
    public RenderBlockOutput render(RenderBlockInput input) {
        RenderBufferRenderBlockInput inp = (RenderBufferRenderBlockInput)input;

        quadShader.bind();
        quadShader.setUniform("texture_sampler", 0);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, inp.renderBuffer.textureId);

        ((DefaultRenderPipeline)renderPipeline).initToEndFullRender(quad, 0);

        glBindTexture(GL_TEXTURE_2D, 0);
        quadShader.unbind();

        return null;
    }

    @Override
    public void cleanUp() {
        quad.cleanUp();
    }
}
