package Kurama.lighting;

import Kurama.Mesh.Mesh;
import Kurama.shadow.ShadowMap;
import Kurama.Math.Matrix;
import Kurama.Math.Quaternion;
import Kurama.Math.Vector;
import Kurama.game.Game;
import Kurama.ComponentSystem.components.model.Model;

import java.util.List;

public class DirectionalLight extends Model {

    public Vector color;
    public float intensity;
    public Vector direction_Vector;
    public float lightPosScale = 100;
    public ShadowMap shadowMap;
    public Matrix shadowProjectionMatrix;
    public boolean doesProduceShadow = false;

    public DirectionalLight(Game game,Vector color, Quaternion direction, float intensity, ShadowMap shadowMap,
                            Mesh mesh,  Matrix shadowProjectionMatrix, String identifier) {

        super(game, mesh, identifier);
        this.color = color;
        this.orientation = direction;
        this.intensity = intensity;
        this.shadowMap = shadowMap;
        this.shadowProjectionMatrix = shadowProjectionMatrix;
    }

    public DirectionalLight(Game game, Vector color, Quaternion direction, float intensity, ShadowMap shadowMap,
                            List<Mesh> meshes, Matrix shadowProjectionMatrix, String identifier) {

        super(game, meshes, identifier);
        this.color = color;
        this.orientation = direction;
        this.intensity = intensity;
        this.shadowMap = shadowMap;
        this.shadowProjectionMatrix = shadowProjectionMatrix;
    }

    public DirectionalLight(DirectionalLight light) {
        this(light.game,new Vector(light.color), new Quaternion(light.orientation), light.intensity, light.shadowMap,
                light.meshes,  light.shadowProjectionMatrix, light.identifier);
        this.doesProduceShadow = light.doesProduceShadow;
        this.shouldSelfCastShadow = light.shouldSelfCastShadow;
        this.objectToWorldMatrix = light.objectToWorldMatrix;
        this.worldToObject = light.worldToObject;
    }
}
