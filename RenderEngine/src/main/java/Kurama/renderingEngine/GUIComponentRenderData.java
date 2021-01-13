package Kurama.renderingEngine;

import Kurama.GUI.Component;
import Kurama.game.Game;
import Kurama.scene.Scene;

public class GUIComponentRenderData extends RenderPipelineData {

    public Component component;
    public GUIComponentRenderData(Scene scene, Game game, Component component) {
        super(scene, game);
        this.component = component;
    }
}
