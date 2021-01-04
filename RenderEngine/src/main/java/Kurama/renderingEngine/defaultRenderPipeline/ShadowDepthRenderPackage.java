package Kurama.renderingEngine.defaultRenderPipeline;

import Kurama.Math.Matrix;

import java.util.List;

public class ShadowDepthRenderPackage {
    public List<Matrix> worldToDirectionalLights;
    public List<Matrix> worldToSpotLights;
    public ShadowDepthRenderPackage(List<Matrix> worldToDirectionalLights,List<Matrix> worldToSpotLights) {
        this.worldToDirectionalLights = worldToDirectionalLights;
        this.worldToSpotLights = worldToSpotLights;
    }
}
