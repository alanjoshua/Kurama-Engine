package Kurama.renderingEngine;

import Kurama.game.Game;
import Kurama.scene.Scene;

public class RenderPipelineInput {

    public Scene scene;
    public RenderPipelineOutput previousOutput;
    public Game game;

    public RenderPipelineInput(Scene scene, Game game, RenderPipelineOutput previousOutput) {
        this.scene = scene;
        this.previousOutput = previousOutput;
        this.game = game;
    }

}
