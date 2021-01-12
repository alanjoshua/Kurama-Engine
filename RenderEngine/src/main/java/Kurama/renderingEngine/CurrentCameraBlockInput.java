package Kurama.renderingEngine;

import Kurama.camera.Camera;
import Kurama.game.Game;
import Kurama.scene.Scene;

public class CurrentCameraBlockInput extends RenderPipelineInput {
    public Camera camera;

    public CurrentCameraBlockInput(Scene scene, Game game, Camera camera, RenderPipelineOutput previousOutput) {
        super(scene, game, previousOutput);
        this.camera = camera;
    }

}
