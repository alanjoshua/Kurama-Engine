package Kurama.renderingEngine;

import Kurama.GUI.components.Component;
import Kurama.game.Game;
import Kurama.scene.Scene;

public class GUIComponentRenderPipelineData extends RenderPipelineData {

    public Component component;

    public GUIComponentRenderPipelineData(Scene scene, Game game, Component masterComponent) {
        super(scene, game);
        this.component = masterComponent;
    }
}
