package Kurama.renderingEngine;

import Kurama.game.Game;
import Kurama.scene.Scene;

import static org.lwjgl.opengl.GL11.*;

public class RenderingEngineGL extends RenderingEngine {

    public RenderPipeline sceneRenderPipeline;
    public RenderPipeline guiRenderPipeline;

    public void init(Scene scene) {
        sceneRenderPipeline.setup(new RenderPipelineInput(scene, null));
        guiRenderPipeline.setup(new RenderPipelineInput(scene, null));
    }

    public RenderingEngineGL(Game game) {
        super(game);
    }

    public static void clear() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    public void render(Scene scene) {
        var output = sceneRenderPipeline.render(new RenderPipelineInput(scene, null));
        guiRenderPipeline.render(new RenderPipelineInput(scene, output));
    }

    public void cleanUp() {
        sceneRenderPipeline.cleanUp();
        guiRenderPipeline.cleanUp();
    }

}

