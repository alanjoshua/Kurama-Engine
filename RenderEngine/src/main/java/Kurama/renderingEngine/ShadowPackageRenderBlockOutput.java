package Kurama.renderingEngine;

import Kurama.renderingEngine.defaultRenderPipeline.ShadowDepthRenderPackage;

public class ShadowPackageRenderBlockOutput extends RenderBlockOutput {

    public ShadowDepthRenderPackage shadowPackage;

    public ShadowPackageRenderBlockOutput(ShadowDepthRenderPackage shadowPackage) {
        this.shadowPackage = shadowPackage;
    }

}
