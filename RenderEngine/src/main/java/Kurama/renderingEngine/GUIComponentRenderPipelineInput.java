package Kurama.renderingEngine;

import Kurama.GUI.Component;
import Kurama.scene.Scene;

public class GUIComponentRenderPipelineInput extends RenderPipelineInput {

    public Component component;

    public GUIComponentRenderPipelineInput(Scene scene, Component masterComponent, RenderPipelineOutput previousOutput) {
        super(scene, previousOutput);
        this.component = masterComponent;
    }
}
