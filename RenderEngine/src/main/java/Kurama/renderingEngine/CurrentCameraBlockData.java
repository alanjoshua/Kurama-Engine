package Kurama.renderingEngine;

import Kurama.camera.Camera;
import Kurama.game.Game;
import Kurama.renderingEngine.defaultRenderPipeline.ShadowDepthRenderPackage;
import Kurama.scene.Scene;

public class CurrentCameraBlockData extends RenderPipelineData {
    public Camera camera;
    public ShadowDepthRenderPackage shadowPackage;
    public CurrentCameraBlockData(Scene scene, Game game, Camera camera, ShadowDepthRenderPackage shadowPackage) {
        super(scene, game);
        this.camera = camera;
        this.shadowPackage = shadowPackage;
    }

}
