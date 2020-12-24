package engine.renderingEngine;

import engine.game.Game;
import engine.scene.Scene;

import static org.lwjgl.opengl.GL11.*;

public class RenderingEngineGL extends RenderingEngine {

    public RenderPipeline renderPipeline;

    public void init(Scene scene) {
        renderPipeline.setup(scene);
    }

    public RenderingEngineGL(Game game) {
        super(game);
    }

    public static void clear() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    public void render(Scene scene) {
        renderPipeline.render(scene);
    }

    public void cleanUp() {
        renderPipeline.cleanUp();
    }

}

