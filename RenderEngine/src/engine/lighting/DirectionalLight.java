package engine.lighting;

import engine.Math.Vector;

public class DirectionalLight {

    public Vector color;
    public Vector direction;
    public float intensity;

    public DirectionalLight(Vector color, Vector direction, float intensity) {
        this.color = color;
        this.direction = direction;
        this.intensity = intensity;
    }

    public DirectionalLight(DirectionalLight light) {
        this(new Vector(light.color), new Vector(light.direction), light.intensity);
    }

}
