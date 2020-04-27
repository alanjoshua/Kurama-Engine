package engine.lighting;

import engine.Math.Vector;

public class SpotLight {

    public float cutOff;
    public PointLight pointLight;
    public Vector coneDirection;

    public SpotLight(PointLight pointLight, Vector coneDirection, float cutOffAngle) {
        this.pointLight = pointLight;
        this.coneDirection = coneDirection;
        setCutOffAngle(cutOffAngle);
    }
    public SpotLight(SpotLight spotLight) {
        this(new PointLight(spotLight.pointLight),
                new Vector(spotLight.coneDirection),
                0);
        cutOff = spotLight.cutOff;
    }


    public final void setCutOffAngle(float cutOffAngle) {
        cutOff = (float)Math.cos(Math.toRadians(cutOffAngle));
    }

}
