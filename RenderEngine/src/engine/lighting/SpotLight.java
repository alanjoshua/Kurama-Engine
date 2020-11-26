package engine.lighting;

import engine.DataStructure.Mesh.Mesh;
import engine.Effects.ShadowMap;
import engine.Math.Matrix;
import engine.Math.Quaternion;
import engine.Math.Vector;
import engine.game.Game;
import engine.model.Model;

import java.util.List;

public class SpotLight extends Model {

    public float cutOff;
    public float angle;
    public PointLight pointLight;
    public Vector coneDirection;
    public ShadowMap shadowMap;
    public Matrix shadowProjectionMatrix;

    public SpotLight(Game game, PointLight pointLight, Quaternion orientation, float angle, ShadowMap shadowMap,
                     Mesh mesh, Mesh boundingBox, Matrix shadowProjectionMatrix, String identifier) {
        super(game,mesh, identifier);
        this.pointLight = pointLight;
        this.angle = angle;
        this.cutOff = (float)Math.cos(Math.toRadians(angle));
        this.shadowMap = shadowMap;
        this.orientation = orientation;
        this.boundingbox = boundingBox;
        this.shadowProjectionMatrix = shadowProjectionMatrix;
    }

    public SpotLight(Game game, PointLight pointLight, Quaternion orientation, float angle, ShadowMap shadowMap,
                     List<Mesh> meshes, Mesh boundingBox, Matrix shadowProjectionMatrix, String identifier) {
        super(game, meshes, identifier);
        this.pointLight = pointLight;
        this.angle = angle;
        this.cutOff = (float)Math.cos(Math.toRadians(angle));
        this.shadowMap = shadowMap;
        this.orientation = orientation;
        this.boundingbox = boundingBox;
        this.shadowProjectionMatrix = shadowProjectionMatrix;
    }

    public SpotLight(SpotLight spotLight) {
        this(spotLight.game,new PointLight(spotLight.pointLight),spotLight.orientation,spotLight.angle,
                spotLight.shadowMap,spotLight.meshes, spotLight.boundingbox, spotLight.shadowProjectionMatrix,
                spotLight.identifier);
        cutOff = spotLight.cutOff;
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
