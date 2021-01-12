package Kurama.renderingEngine;

import Kurama.GUI.Component;
import Kurama.game.Game;
import Kurama.scene.Scene;

import static org.lwjgl.opengl.GL11C.*;

public class RenderingEngineGL extends RenderingEngine {

    public RenderPipeline sceneRenderPipeline;
    public RenderPipeline guiRenderPipeline;

    public void init(Scene scene) {
        sceneRenderPipeline.setup(new RenderPipelineInput(scene, game, null));
        guiRenderPipeline.setup(new RenderPipelineInput(scene, game, null));
    }

    public RenderingEngineGL(Game game) {
        super(game);
    }

    public static void clear() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    public void render(Scene scene, Component masterWindow) {
        var output = sceneRenderPipeline.render(new RenderPipelineInput(scene, game,null));
        guiRenderPipeline.render(new GUIComponentRenderPipelineInput(scene, game, masterWindow, output));
    }

    public void cleanUp() {
        sceneRenderPipeline.cleanUp();
        guiRenderPipeline.cleanUp();
    }

}

