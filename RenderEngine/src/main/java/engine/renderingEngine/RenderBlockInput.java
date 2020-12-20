package engine.renderingEngine;

import engine.game.Game;
import engine.scene.Scene;

public class RenderBlockInput {
    public Scene scene;
    public Game game;

    public RenderBlockInput(Scene scene, Game game) {
        this.game = game;
        this.scene = scene;
    }
}
