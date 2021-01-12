package Kurama.renderingEngine.ginchan;

import Kurama.Mesh.Mesh;
import Kurama.game.Game;
import Kurama.renderingEngine.*;
import Kurama.renderingEngine.defaultRenderPipeline.DefaultRenderPipeline;

import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL11C.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.glEnable;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

public class Gintoki extends RenderPipeline {

    public static String rectangleShaderBlockID = "rectangleBlock";
    public RectangleShaderBlock rectangleShaderBlock = new RectangleShaderBlock(game, this, rectangleShaderBlockID);

    public Gintoki(Game game, RenderPipeline parentPipeline, String pipelineID) {
        super(game, parentPipeline, pipelineID);
    }

    @Override
    public void setup(RenderPipelineInput input) {
        rectangleShaderBlock.setup(new RenderPipelineInput(input.scene, game, null));
        renderBlocks.add(rectangleShaderBlock);
    }

    @Override
    public RenderPipelineOutput render(RenderPipelineInput input) {

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0,0, game.getMasterWindow().width, game.getMasterWindow().height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        GUIComponentRenderPipelineInput inp = (GUIComponentRenderPipelineInput) input;

        glDisable(GL_DEPTH_TEST);
        glDepthMask(false);
        glDisable(GL_CULL_FACE);

        rectangleShaderBlock.render(new GUIComponentRenderInput(input.scene, game, inp.component, null));

        glEnable(GL_DEPTH_TEST);
        glDepthMask(true);
        glEnable(GL_CULL_FACE);

        return null;
    }

    @Override
    public void cleanUp() {
        rectangleShaderBlock.cleanUp();
    }

    @Override
    public void initializeMesh(Mesh mesh) {
        DefaultRenderPipeline.initializeRegularMesh(mesh);
    }
}
