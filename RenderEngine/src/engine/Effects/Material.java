package engine.Effects;

import engine.DataStructure.Texture;
import engine.Math.Vector;
import engine.model.Terrain;

// Inspired by LWJGL book

public class Material {

    public static Vector DEFAULTCOLOR = new Vector(new float[]{1,1,1,1});
    public Texture texture;
    public Texture normalMap;
    public Texture diffuseMap;
    public Texture specularMap;
    public Vector ambientColor;
    public Vector diffuseColor;
    public Vector specularColor;
    public float reflectance = 1f;
    public float specularPower = 10;

    public Material() {
        this.ambientColor = DEFAULTCOLOR;
        this.diffuseColor = DEFAULTCOLOR;
        this.specularColor = new Vector(0,0,0,1);
        this.texture = null;
        this.normalMap = null;
        this.diffuseMap = null;
        this.specularMap = null;
    }

    public Material(Vector colour, float reflectance) {
        this(colour, colour, colour, null, null, null,null,reflectance);
    }

    public Material(Texture texture) {
        this(DEFAULTCOLOR, DEFAULTCOLOR, DEFAULTCOLOR, texture, null, null,null, 0);
    }

    public Material(Texture texture, float reflectance) {
        this(DEFAULTCOLOR, DEFAULTCOLOR, DEFAULTCOLOR, texture, null,null,null, reflectance);
    }

    public Material(Vector ambientColour, Vector diffuseColour, Vector specularColour, Texture texture, Texture normalMap, Texture diffuseMap,Texture specularMap,float reflectance) {
        this.ambientColor = ambientColour;
        this.diffuseColor = diffuseColour;
        this.specularColor = specularColour;
        this.texture = texture;
        this.reflectance = reflectance;
        this.normalMap = normalMap;
        this.specularMap = specularMap;
        this.diffuseMap = diffuseMap;
    }

}
