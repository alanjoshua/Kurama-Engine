package Kurama.renderingEngine;

import Kurama.game.Game;
import Kurama.scene.Scene;

public class RenderPipelineData {

    public Scene scene;
    public Game game;

    public RenderPipelineData(Scene scene, Game game) {
        this.scene = scene;
        this.game = game;
    }

}
