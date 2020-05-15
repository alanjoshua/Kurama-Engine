package engine.Effects;

import engine.DataStructure.Texture;
import engine.Math.Vector;

// Inspired by LWJGL book

public class Material {

    public static Vector DEFAULTCOLOR = new Vector(new float[]{1,1,1,1});
    public Texture texture;
    public Vector ambientColor;
    public Vector diffuseColor;
    public Vector specularColor;
    public float reflectance;

    public Material() {
        this.ambientColor = DEFAULTCOLOR;
        this.diffuseColor = DEFAULTCOLOR;
        this.specularColor = DEFAULTCOLOR;
        this.texture = null;
        this.reflectance = 0;
    }

    public Material(Vector colour, float reflectance) {
        this(colour, colour, colour, null, reflectance);
    }

    public Material(Texture texture) {
        this(DEFAULTCOLOR, DEFAULTCOLOR, DEFAULTCOLOR, texture, 0);
    }

    public Material(Texture texture, float reflectance) {
        this(DEFAULTCOLOR, DEFAULTCOLOR, DEFAULTCOLOR, texture, reflectance);
    }

    public Material(Vector ambientColour, Vector diffuseColour, Vector specularColour, Texture texture, float reflectance) {
        this.ambientColor = ambientColour;
        this.diffuseColor = diffuseColour;
        this.specularColor = specularColour;
        this.texture = texture;
        this.reflectance = reflectance;
    }

}
