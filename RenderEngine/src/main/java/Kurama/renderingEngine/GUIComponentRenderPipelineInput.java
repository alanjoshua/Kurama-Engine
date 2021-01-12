package Kurama.renderingEngine;

import Kurama.GUI.Component;
import Kurama.game.Game;
import Kurama.scene.Scene;

public class GUIComponentRenderPipelineInput extends RenderPipelineInput {

    public Component component;

    public GUIComponentRenderPipelineInput(Scene scene, Game game, Component masterComponent, RenderPipelineOutput previousOutput) {
        super(scene, game, previousOutput);
        this.component = masterComponent;
    }
}
