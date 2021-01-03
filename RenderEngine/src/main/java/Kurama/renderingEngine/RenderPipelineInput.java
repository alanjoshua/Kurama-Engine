package Kurama.renderingEngine;

import Kurama.scene.Scene;

public class RenderPipelineInput {

    public Scene scene;
    public RenderPipelineOutput previousOutput;

    public RenderPipelineInput(Scene scene, RenderPipelineOutput previousOutput) {
        this.scene = scene;
        this.previousOutput = previousOutput;
    }

}
