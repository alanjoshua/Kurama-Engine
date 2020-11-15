package engine.Effects;

import engine.DataStructure.Texture;
import engine.Math.Vector;
import engine.model.Terrain;

// Inspired by LWJGL book

public class Material {

    public static Vector DEFAULTCOLOR = new Vector(new float[]{1,1,1,1});
    public static float DEFAULT_SPECULAR_POWER = 10;
    public static float DEFAULT_REFLECTANCE = 1;

    public Texture texture;
    public Texture normalMap;
    public Texture diffuseMap;
    public Texture specularMap;
    public Vector ambientColor;
    public Vector diffuseColor;
    public Vector specularColor;
    public float reflectance = DEFAULT_REFLECTANCE;
    public float specularPower = DEFAULT_SPECULAR_POWER;
    public String matName;

    public Material() {
        this.ambientColor = DEFAULTCOLOR;
        this.diffuseColor = DEFAULTCOLOR;
        this.specularColor = new Vector(0,0,0,1);
        this.texture = null;
        this.normalMap = null;
        this.diffuseMap = null;
        this.specularMap = null;
        this.matName = "DEFAULT";
    }

    public Material(String matName) {
        this.ambientColor = DEFAULTCOLOR;
        this.diffuseColor = DEFAULTCOLOR;
        this.specularColor = new Vector(0,0,0,1);
        this.texture = null;
        this.normalMap = null;
        this.diffuseMap = null;
        this.specularMap = null;
        this.matName = matName;
    }

    public Material(Vector colour, float reflectance, String matName) {
        this(colour, colour, colour, null, null, null,null,reflectance, DEFAULT_SPECULAR_POWER, matName);
    }

    public Material(Texture texture, String matName) {
        this(DEFAULTCOLOR, DEFAULTCOLOR, DEFAULTCOLOR, texture, null, null,null, DEFAULT_REFLECTANCE, DEFAULT_SPECULAR_POWER, matName);
    }

    public Material(Texture texture, float reflectance, String matName) {
        this(DEFAULTCOLOR, DEFAULTCOLOR, DEFAULTCOLOR, texture, null,null,null, reflectance, DEFAULT_SPECULAR_POWER, matName);
    }

    public Material(Vector ambientColour, Vector diffuseColour, Vector specularColour, Texture texture,
                    Texture normalMap, Texture diffuseMap,Texture specularMap,float reflectance, float specularPower, String matName) {
        this.ambientColor = ambientColour;
        this.diffuseColor = diffuseColour;
        this.specularColor = specularColour;
        this.texture = texture;
        this.reflectance = reflectance;
        this.normalMap = normalMap;
        this.specularMap = specularMap;
        this.diffuseMap = diffuseMap;
        this.specularPower = specularPower;
        this.matName = matName;
    }

    public boolean equals(Material m) {
        return (ambientColor.equals(m.ambientColor)) && (diffuseColor.equals(m.diffuseColor)) && (specularColor.equals(m.specularColor))
                && (texture.equals(m.texture)) && (reflectance == m.reflectance) && (normalMap.equals(m.normalMap)) && (specularMap.equals(m.specularMap))
                && (diffuseColor.equals(m.diffuseColor)) && (specularPower == m.specularPower) && (matName.equals(m.matName));
    }

}
