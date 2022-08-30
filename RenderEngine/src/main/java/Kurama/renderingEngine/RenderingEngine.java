package Kurama.renderingEngine;

import Kurama.Vulkan.Renderable;
import Kurama.game.Game;
import Kurama.scene.Scene;

public abstract class RenderingEngine {

    protected Game game;

    public enum ProjectionMode {
        ORTHO, PERSPECTIVE
    }

    public ProjectionMode projectionMode = ProjectionMode.PERSPECTIVE;

    public RenderingEngine(Game game) { this.game = game; }

    public abstract void init(Scene scene);
    public abstract void cleanUp();
}
