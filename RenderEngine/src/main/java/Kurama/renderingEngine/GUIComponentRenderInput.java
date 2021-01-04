package Kurama.renderingEngine;

import Kurama.GUI.Component;
import Kurama.game.Game;
import Kurama.scene.Scene;

public class GUIComponentRenderInput extends RenderBlockInput {

    public Component component;
    public GUIComponentRenderInput(Scene scene, Game game, Component component, RenderBlockOutput previousOutput) {
        super(scene, game, previousOutput);
        this.component = component;
    }
}
