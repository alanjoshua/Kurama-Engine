package Kurama.renderingEngine.ginchan;

import Kurama.Math.Vector;
import Kurama.Mesh.Mesh;
import Kurama.game.Game;
import Kurama.renderingEngine.RenderBlockInput;
import Kurama.renderingEngine.RenderPipeline;
import Kurama.renderingEngine.RenderPipelineInput;

public class Gintoki extends RenderPipeline {

    public static String rectangleShaderBlockID = "rectangleBlock";
    public RectangleShaderBlock rectangleShaderBlock = new RectangleShaderBlock(rectangleShaderBlockID, this);

    public Gintoki(Game game) {
        super(game);
    }

    @Override
    public void setup(RenderPipelineInput input) {
        rectangleShaderBlock.setup(new RenderBlockInput(input.scene, game));
    }

    @Override
    public void render(RenderPipelineInput input) {
        rectangleShaderBlock.render(new RenderBlockInput(input.scene, game));
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
