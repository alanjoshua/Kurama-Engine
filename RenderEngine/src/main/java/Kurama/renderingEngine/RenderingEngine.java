package Kurama.renderingEngine;

import Kurama.ComponentSystem.components.model.Model;
import Kurama.game.Game;
import Kurama.scene.Scene;

import java.util.ArrayList;
import java.util.List;

public abstract class RenderingEngine {

    protected Game game;
    public List<Model> models = new ArrayList<>();

    public enum ProjectionMode {
        ORTHO, PERSPECTIVE
    }

    public ProjectionMode projectionMode = ProjectionMode.PERSPECTIVE;

    public RenderingEngine(Game game) { this.game = game; }

    public abstract void init(Scene scene);
    public abstract void cleanUp();
    public abstract void tick();
}
