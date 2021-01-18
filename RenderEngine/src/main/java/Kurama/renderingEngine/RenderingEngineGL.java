package Kurama.renderingEngine;

import Kurama.GUI.components.Component;
import Kurama.game.Game;
import Kurama.scene.Scene;

import static org.lwjgl.opengl.GL11C.*;

public class RenderingEngineGL extends RenderingEngine {

    public RenderPipeline sceneRenderPipeline;
    public RenderPipeline guiRenderPipeline;

    public void init(Scene scene) {
        sceneRenderPipeline.setup(new RenderPipelineData(scene, game));
        guiRenderPipeline.setup(new RenderPipelineData(scene, game));
    }

    public RenderingEngineGL(Game game) {
        super(game);
    }

    public static void clear() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    public void render(Scene scene, Component masterWindow) {
        sceneRenderPipeline.render(new RenderPipelineData(scene, game));
        guiRenderPipeline.render(new GUIComponentRenderPipelineData(scene, game, masterWindow));
    }

    public void cleanUp() {
        sceneRenderPipeline.cleanUp();
        guiRenderPipeline.cleanUp();
    }

}

