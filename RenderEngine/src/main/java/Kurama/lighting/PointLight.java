package Kurama.lighting;

import Kurama.Math.Vector;

public class PointLight {

    public static class Attenuation {

        public float constant;
        public float linear;
        public float exponent;

        public Attenuation(float constant, float linear, float exp) {
            this.constant = constant;
            this.linear = linear;
            this.exponent = exp;
        }

    }

    public Vector color;
    public Vector pos;
    public float intensity;
    public Attenuation attenuation;
    public boolean doesProduceShadow = false;

    public PointLight(Vector color, Vector position, float intensity) {
        attenuation = new Attenuation(1, 0, 0);
        this.color = color;
        this.pos = position;
        this.intensity = intensity;
    }

    public PointLight(Vector color, Vector position, float intensity, Attenuation attenuation) {
        this(color, position, intensity);
        this.attenuation = attenuation;
    }

    public PointLight(PointLight pointLight) {
        this(new Vector(pointLight.color), new Vector(pointLight.pos), pointLight.intensity, pointLight.attenuation);
        this.doesProduceShadow = pointLight.doesProduceShadow;
    }

}
