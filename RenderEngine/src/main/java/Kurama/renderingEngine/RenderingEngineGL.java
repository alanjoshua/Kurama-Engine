package Kurama.renderingEngine;

import Kurama.game.Game;
import Kurama.scene.Scene;

import static org.lwjgl.opengl.GL11.*;

public class RenderingEngineGL extends RenderingEngine {

    public RenderPipeline sceneRenderPipeline;
    public RenderPipeline guiRenderPipeline;

    public void init(Scene scene) {
        sceneRenderPipeline.setup(new RenderPipelineInput(scene));
        guiRenderPipeline.setup(new RenderPipelineInput(scene));
    }

    public RenderingEngineGL(Game game) {
        super(game);
    }

    public static void clear() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    public void render(Scene scene) {
        sceneRenderPipeline.render(new RenderPipelineInput(scene));
        guiRenderPipeline.render(new RenderPipelineInput(scene));
    }

    public void cleanUp() {
        sceneRenderPipeline.cleanUp();
        guiRenderPipeline.cleanUp();
    }

}

