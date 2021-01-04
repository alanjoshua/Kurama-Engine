package Kurama.renderingEngine;

import Kurama.game.Game;
import Kurama.scene.Scene;

public class RenderBlockInput {
    public Scene scene;
    public Game game;
    public RenderBlockOutput previousOutput;

    public RenderBlockInput(Scene scene, Game game, RenderBlockOutput previousOutput) {
        this.game = game;
        this.scene = scene;
        this.previousOutput = previousOutput;
    }
}
