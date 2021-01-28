package Kurama.lighting;

import Kurama.Mesh.Mesh;
import Kurama.shadow.ShadowMap;
import Kurama.Math.Matrix;
import Kurama.Math.Quaternion;
import Kurama.Math.Vector;
import Kurama.game.Game;
import Kurama.ComponentSystem.components.model.Model;

import java.util.List;

public class SpotLight extends Model {

    public float cutOff;
    public float angle;
    public PointLight pointLight;
    public Vector coneDirection;
    public ShadowMap shadowMap;
    public Matrix shadowProjectionMatrix;
    public boolean doesProduceShadow = false;

    public SpotLight(Game game, PointLight pointLight, Quaternion orientation, float angle, ShadowMap shadowMap,
                     Mesh mesh, Matrix shadowProjectionMatrix, String identifier) {
        super(game,mesh, identifier);
        this.pointLight = pointLight;
        this.angle = angle;
        this.cutOff = (float)Math.cos(Math.toRadians(angle));
        this.shadowMap = shadowMap;
        this.orientation = orientation;
        this.shadowProjectionMatrix = shadowProjectionMatrix;
    }

    public SpotLight(Game game, PointLight pointLight, Quaternion orientation, float angle, ShadowMap shadowMap,
                     List<Mesh> meshes, Matrix shadowProjectionMatrix, String identifier) {
        super(game, meshes, identifier);
        this.pointLight = pointLight;
        this.angle = angle;
        this.cutOff = (float)Math.cos(Math.toRadians(angle));
        this.shadowMap = shadowMap;
        this.orientation = orientation;
        this.shadowProjectionMatrix = shadowProjectionMatrix;
    }

    public SpotLight(SpotLight spotLight) {
        this(spotLight.game,new PointLight(spotLight.pointLight),spotLight.orientation,spotLight.angle,
                spotLight.shadowMap,spotLight.meshes, spotLight.shadowProjectionMatrix,
                spotLight.identifier);
        cutOff = spotLight.cutOff;
        this.doesProduceShadow = spotLight.doesProduceShadow;
        this.shouldSelfCastShadow = spotLight.shouldSelfCastShadow;
        this.objectToWorldMatrix = spotLight.objectToWorldMatrix;
        this.worldToObject = spotLight.worldToObject;
    }

    public void setPos(Vector newPos) {
        this.pos = newPos;
        this.pointLight.pos = newPos;
    }

    public Matrix generateShadowProjectionMatrix(float n, float f, float x, float y) {
        float aspectRatio = (float) shadowMap.shadowMapWidth / (float) shadowMap.shadowMapHeight;
        Matrix projMatrix = Matrix.buildPerspectiveMatrix(angle*2, aspectRatio, n, f, x, y);
        shadowProjectionMatrix = projMatrix;
        return projMatrix;
    }

}
