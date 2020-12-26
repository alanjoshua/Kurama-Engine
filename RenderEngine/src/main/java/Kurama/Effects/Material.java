package Kurama.Effects;

import Kurama.Math.Vector;
import Kurama.utils.Utils;

// Inspired by LWJGL book

public class Material {

    public static Vector DEFAULTAMBIENTCOLOR = new Vector(new float[]{1,1,1,1});
    public static Vector DEFAULTDIFFUSECOLOR = new Vector(new float[]{1f,1f,1f,1f});
    public static Vector DEFAULTSPECULARCOLOR = new Vector(new float[]{1,1,1,1});
    public static float DEFAULT_SPECULAR_POWER = 10f;
    public static float DEFAULT_REFLECTANCE = 1f;
    public static String DEFAULT_MATERIAL_NAME = "DEFAULT";

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
    public int globalSceneID;

    public static Material DEFAULT_MATERIAL = new Material(DEFAULTAMBIENTCOLOR, DEFAULTAMBIENTCOLOR, DEFAULTAMBIENTCOLOR, null,
            null,null,null, DEFAULT_REFLECTANCE, DEFAULT_SPECULAR_POWER, DEFAULT_MATERIAL_NAME);

    public Material() {
        this.ambientColor = DEFAULTAMBIENTCOLOR;
        this.diffuseColor = DEFAULTDIFFUSECOLOR;
        this.specularColor = DEFAULTSPECULARCOLOR;
        this.texture = null;
        this.normalMap = null;
        this.diffuseMap = null;
        this.specularMap = null;
        this.matName = Utils.getUniqueID();
    }

//    Shallow Copy
//    public Material(Material mat) {
//        this.ambientColor = new Vector(mat.ambientColor);
//        this.diffuseColor = new Vector(mat.diffuseColor);
//        this.specularColor = new Vector(mat.specularColor);
//        this.texture = mat.texture;
//        this.diffuseMap = mat.diffuseMap;
//        this.specularMap = mat.specularMap;
//        this.matName = mat.matName;
//        this.globalSceneID = mat.globalSceneID;
//    }

    public Material(String matName) {
        this.ambientColor = DEFAULTAMBIENTCOLOR;
        this.diffuseColor = DEFAULTDIFFUSECOLOR;
        this.specularColor = DEFAULTSPECULARCOLOR;
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
        this(DEFAULTAMBIENTCOLOR, DEFAULTDIFFUSECOLOR, DEFAULTSPECULARCOLOR, texture, null, null,null, DEFAULT_REFLECTANCE, DEFAULT_SPECULAR_POWER, matName);
    }

    public Material(Texture texture, float reflectance, String matName) {
        this(DEFAULTAMBIENTCOLOR, DEFAULTDIFFUSECOLOR, DEFAULTSPECULARCOLOR, texture, null,null,null, reflectance, DEFAULT_SPECULAR_POWER, matName);
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

    @Override
    public boolean equals(Object obj) {

        if (getClass() != obj.getClass())
            return false;

        Material m = (Material)obj;

        boolean res = (ambientColor.equals(m.ambientColor)) && (diffuseColor.equals(m.diffuseColor)) && (specularColor.equals(m.specularColor))
                && ((texture == null && m.texture == null) || (texture.equals(m.texture))) && (reflectance == m.reflectance) &&
                ((normalMap == null && m.normalMap == null) || (normalMap.equals(m.normalMap))) &&
                ((specularMap == null && m.specularMap == null) || (specularMap.equals(m.specularMap))) &&
                ((diffuseMap == null && m.diffuseMap == null) || (diffuseMap.equals(m.diffuseMap)))
                && (diffuseColor.equals(m.diffuseColor)) && (specularPower == m.specularPower) && (matName.equals(m.matName));

        return res;
    }

    @Override
    public int hashCode() {
        Double ref = (double) reflectance;
        int decLen = ref.toString().split("\\.")[1].length();
        if (decLen >= 1)
            ref = ref * decLen;
        int ref_int = ref.intValue();

        Double spec = (double) specularPower;
        decLen = spec.toString().split("\\.")[1].length();
        if (decLen >= 1)
            spec = spec * decLen;
        int spec_int = spec.intValue();

//        System.out.println("Ref int: "+ref_int + " Spec int: "+spec_int);

        int result = (texture == null ? 0:texture.getId()) +  (normalMap == null ? 0:normalMap.getId()) +
                (diffuseMap == null ? 0:diffuseMap.getId()) + (specularMap == null ? 0:specularMap.getId()) +
                ambientColor.hashCode() + diffuseColor.hashCode() + specularColor.hashCode() +
                ref_int + spec_int + matName.hashCode();

        return result;
    }

    @Override
    public String toString() {
        String res = "texture: "+texture +  " normalMap: " + normalMap + " diffuseMap: "+diffuseMap + " specularMap: "+specularMap+
                " ambientColor: "+ambientColor + " diffuseColor: "+diffuseColor + " specularColor: "+specularColor +
                " reflectance: " +reflectance+" specularPower: "+specularPower+" matName: "+ matName;
        return res;
    }
}
