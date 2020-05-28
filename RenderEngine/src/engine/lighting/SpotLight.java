package engine.lighting;

import engine.DataStructure.Mesh.Mesh;
import engine.Effects.ShadowMap;
import engine.Math.Quaternion;
import engine.Math.Vector;
import engine.game.Game;
import engine.model.Model;

public class SpotLight extends Model {

    public float cutOff;
    public float angle;
    public PointLight pointLight;
    public Vector coneDirection;
    public ShadowMap shadowMap;

    public SpotLight(Game game, PointLight pointLight, Quaternion orientation, float angle, ShadowMap shadowMap, Mesh mesh, String identifier) {
        super(game,mesh, identifier);
        this.pointLight = pointLight;
        this.angle = angle;
        this.cutOff = (float)Math.cos(Math.toRadians(angle));
        this.shadowMap = shadowMap;
        this.orientation = orientation;
    }
    public SpotLight(SpotLight spotLight) {
        this(spotLight.game,new PointLight(spotLight.pointLight),spotLight.orientation,spotLight.angle,spotLight.shadowMap,spotLight.mesh,spotLight.identifier);
        cutOff = spotLight.cutOff;
    }

    public void setPos(Vector newPos) {
        this.pos = newPos;
        this.pointLight.pos = newPos;
    }


//    public final void setCutOffAngle(float cutOffAngle) {
//        cutOff = (float)Math.cos(Math.toRadians(cutOffAngle));
//    }

}
