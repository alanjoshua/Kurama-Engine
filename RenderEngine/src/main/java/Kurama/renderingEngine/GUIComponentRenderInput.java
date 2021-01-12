package Kurama.renderingEngine;

import Kurama.GUI.Component;
import Kurama.game.Game;
import Kurama.scene.Scene;

public class GUIComponentRenderInput extends RenderPipelineInput {

    public Component component;
    public GUIComponentRenderInput(Scene scene, Game game, Component component, RenderPipelineOutput previousOutput) {
        super(scene, game, previousOutput);
        this.component = component;
    }
}
