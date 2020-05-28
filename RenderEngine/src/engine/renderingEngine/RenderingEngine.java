package engine.renderingEngine;

import engine.Math.Matrix;
import engine.Math.Vector;
import engine.game.Game;
import engine.lighting.DirectionalLight;
import engine.lighting.PointLight;
import engine.lighting.SpotLight;

import java.util.List;
import java.util.stream.Collectors;

public abstract class RenderingEngine {

    public class LightDataPackage {
        public PointLight pointLights[];
        public SpotLight spotLights[];
        public DirectionalLight directionalLights[];
    }

    protected Game game;

    public enum ProjectionMode {
        ORTHO, PERSPECTIVE
    }

    public enum RenderPipeline {
        Matrix, Quat
    }

    protected ProjectionMode projectionMode = ProjectionMode.PERSPECTIVE;
    protected RenderPipeline renderPipeline = RenderPipeline.Matrix;

    public RenderingEngine(Game game) {
        this.game = game;
    }

    public abstract void init();
    public abstract void cleanUp();

    public RenderingEngine.LightDataPackage processLights(List<PointLight> pointLights, List<SpotLight> spotLights, List<DirectionalLight> directionalLights, Matrix worldToCam) {
        List<PointLight> pointLightsRes;
        List<SpotLight> spotLightsRes;
        List<DirectionalLight> directionalLightsRes;

        pointLightsRes = pointLights.stream()
                .map(l -> {
                    PointLight currLight = new PointLight(l);
                    currLight.pos = worldToCam.matMul(currLight.pos.addDimensionToVec(1)).getColumn(0).removeDimensionFromVec(3);
                    return currLight;
                })
                .collect(Collectors.toList());

        directionalLightsRes = directionalLights.stream()
                .map(l -> {
                    DirectionalLight currDirectionalLight = new DirectionalLight(l);
                    currDirectionalLight.direction_Vector = worldToCam.matMul(currDirectionalLight.getOrientation().getRotationMatrix().getColumn(2).scalarMul(-1).addDimensionToVec(0)).getColumn(0).removeDimensionFromVec(3);
                    return currDirectionalLight;
                })
                .collect(Collectors.toList());

        spotLightsRes = spotLights.stream()
                .map(l -> {
                    SpotLight currSpotLight = new SpotLight(l);
                    //Vector dir = new Vector(currSpotLight.coneDirection).addDimensionToVec(0);
                    currSpotLight.coneDirection = worldToCam.matMul(currSpotLight.getOrientation().getRotationMatrix().getColumn(2).scalarMul(-1).addDimensionToVec(0)).getColumn(0).removeDimensionFromVec(3);

                    Vector spotLightPos = currSpotLight.pointLight.pos;
                    Vector auxSpot = new Vector(spotLightPos).addDimensionToVec(1);
                    currSpotLight.pointLight.pos = worldToCam.matMul(auxSpot).getColumn(0).removeDimensionFromVec(3);

                    return currSpotLight;
                })
                .collect(Collectors.toList());

        LightDataPackage res = new LightDataPackage();

        res.pointLights = new PointLight[pointLightsRes.size()];
        pointLightsRes.toArray(res.pointLights);

        res.spotLights = new SpotLight[spotLightsRes.size()];
        spotLightsRes.toArray(res.spotLights);

        res.directionalLights = new DirectionalLight[directionalLightsRes.size()];
        directionalLightsRes.toArray(res.directionalLights);

        return res;
    }

    public ProjectionMode getProjectionMode() {
        return projectionMode;
    }

    public void setProjectionMode(ProjectionMode projectionMode) {
        this.projectionMode = projectionMode;
    }

    public RenderPipeline getRenderPipeline() {
        return renderPipeline;
    }

    public void setRenderPipeline(RenderPipeline renderPipeline) {
        this.renderPipeline = renderPipeline;
    }
}
