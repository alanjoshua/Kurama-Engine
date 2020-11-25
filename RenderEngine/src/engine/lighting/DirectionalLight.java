package engine.lighting;

import engine.DataStructure.Mesh.Mesh;
import engine.Effects.ShadowMap;
import engine.Math.Matrix;
import engine.Math.Quaternion;
import engine.Math.Vector;
import engine.game.Game;
import engine.model.Model;
import engine.model.ModelBehaviourTickInput;

public class DirectionalLight extends Model {

    public Vector color;
    public float intensity;
    public Vector direction_Vector;
    public float lightPosScale = 100;
    public ShadowMap shadowMap;
    public Matrix shadowProjectionMatrix;

    public DirectionalLight(Game game,Vector color, Quaternion direction, float intensity, ShadowMap shadowMap,
                            Mesh mesh, Mesh boundingBox,  Matrix shadowProjectionMatrix, String identifier) {
        super(game, mesh, identifier, false);
        this.color = color;
        this.orientation = direction;
        this.intensity = intensity;
        this.shadowMap = shadowMap;
        this.boundingbox = boundingBox;
        this.shadowProjectionMatrix = shadowProjectionMatrix;
    }

    public DirectionalLight(DirectionalLight light) {
        this(light.game,new Vector(light.color), new Quaternion(light.orientation), light.intensity, light.shadowMap,
                light.mesh, light.boundingbox,  light.shadowProjectionMatrix, light.identifier);
    }

    @Override
    public void tick(ModelBehaviourTickInput params) {
        pos = orientation.getRotationMatrix().getColumn(2).scalarMul(-lightPosScale);
    }

}
