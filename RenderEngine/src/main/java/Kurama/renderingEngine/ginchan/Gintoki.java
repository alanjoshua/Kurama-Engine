package Kurama.renderingEngine.ginchan;

import Kurama.Math.Vector;
import Kurama.Mesh.Mesh;
import Kurama.game.Game;
import Kurama.renderingEngine.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11C.glEnable;

public class Gintoki extends RenderPipeline {

    public static String rectangleShaderBlockID = "rectangleBlock";
    public RectangleShaderBlock rectangleShaderBlock = new RectangleShaderBlock(rectangleShaderBlockID, this);

    public Gintoki(Game game) {
        super(game);
    }

    @Override
    public void setup(RenderPipelineInput input) {
        rectangleShaderBlock.setup(new RenderBlockInput(input.scene, game, null));
    }

    @Override
    public RenderPipelineOutput render(RenderPipelineInput input) {
        var prevOut = (RenderBufferRenderPipelineOutput)input.previousOutput;

        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        rectangleShaderBlock.render(new RenderBufferRenderBlockInput(input.scene, game, null, prevOut.buffer));
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);

        return null;
    }

    @Override
    public void cleanUp() {
        rectangleShaderBlock.cleanUp();
    }

    @Override
    public void initializeMesh(Mesh mesh) {

    }

    @Override
    public void renderResolutionChanged(Vector renderResolution) {

    }
}
