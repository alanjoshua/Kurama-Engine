package Kurama.lighting;

import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.components.model.SceneComponent;
import Kurama.Math.Vector;
import Kurama.game.Game;

public class PointLight extends SceneComponent {

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

    public PointLight(Game game, Component parent, String identifier, Vector color, Vector position, float intensity) {
        super(game, parent, identifier);
        attenuation = new Attenuation(1, 0, 0);
        this.color = color;
        this.pos = position;
        this.intensity = intensity;
    }

    public PointLight(Game game, Component parent, String identifier, Vector color, Vector position, float intensity, Attenuation attenuation) {
        this(game, parent, identifier, color, position, intensity);
        this.attenuation = attenuation;
    }

    public PointLight(PointLight pointLight) {
        this(pointLight.game, pointLight.parent, pointLight.identifier, new Vector(pointLight.color), new Vector(pointLight.pos), pointLight.intensity, pointLight.attenuation);
        this.doesProduceShadow = pointLight.doesProduceShadow;
    }

}
