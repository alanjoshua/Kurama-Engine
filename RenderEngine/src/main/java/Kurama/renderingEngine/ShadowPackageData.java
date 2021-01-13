package Kurama.renderingEngine;

import Kurama.game.Game;
import Kurama.renderingEngine.defaultRenderPipeline.ShadowDepthRenderPackage;
import Kurama.scene.Scene;

public class ShadowPackageData extends RenderPipelineData {

    public ShadowDepthRenderPackage shadowPackage;

    public ShadowPackageData(Scene scene, Game game, ShadowDepthRenderPackage shadowPackage) {
        super(scene, game);
        this.shadowPackage = shadowPackage;
    }

}
