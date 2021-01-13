package Kurama.renderingEngine.defaultRenderPipeline;

import Kurama.Mesh.Mesh;
import Kurama.game.Game;
import Kurama.geometry.MeshBuilder;
import Kurama.renderingEngine.RenderBufferData;
import Kurama.renderingEngine.RenderPipeline;
import Kurama.renderingEngine.RenderPipelineData;
import Kurama.shader.ShaderProgram;

import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.glBindTexture;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

public class FullScreenQuadBlock extends RenderPipeline {

    private static String quadShaderID = "particleShader";
    private ShaderProgram quadShader;
    private Mesh quad;

    public FullScreenQuadBlock(Game game, RenderPipeline parentPipeline, String pipelineID) {
        super(game, parentPipeline, pipelineID);
    }

    @Override
    public void setup(RenderPipelineData input) {
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
        parentPipeline.initializeMesh(quad);
    }

    @Override
    public RenderPipelineData render(RenderPipelineData input) {
        RenderBufferData inp = (RenderBufferData)input;

        quadShader.bind();
        quadShader.setUniform("texture_sampler", 0);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, inp.renderBuffer.textureId);

        DefaultRenderPipeline.initToEndFullRender(quad, 0);

        glBindTexture(GL_TEXTURE_2D, 0);
        quadShader.unbind();

        return input;
    }

    @Override
    public void cleanUp() {
        quad.cleanUp();
    }
}
