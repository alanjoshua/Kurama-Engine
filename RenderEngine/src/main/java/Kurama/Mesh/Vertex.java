package Kurama.Mesh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static Kurama.Mesh.Mesh.VERTATTRIB.NORMAL;
import static Kurama.Mesh.Mesh.VERTATTRIB.TEXTURE;
import static Kurama.Mesh.MeshletGen.getMortonCode;

public class Vertex {

    public static final int POSITION = 0;
    public static final int TEXTURE = 1;
    public static final int NORMAL = 2;
    public static final int COLOR = 3;
    public static final int TANGENT = 4;
    public static final int BITANGENT = 5;
    public static final int MATERIAL = 6;
    public static final int WEIGHTBIASESPERVERT = 7;
    public static final int JOINTINDICESPERVERT = 8;

    private static Map<Mesh.VERTATTRIB, Integer> attribToIntMapping = new HashMap<>();
    {
        attribToIntMapping.put(Mesh.VERTATTRIB.POSITION, 0);
        attribToIntMapping.put(Mesh.VERTATTRIB.TEXTURE, 1);
        attribToIntMapping.put(Mesh.VERTATTRIB.NORMAL, 2);
        attribToIntMapping.put(Mesh.VERTATTRIB.COLOR, 3);
        attribToIntMapping.put(Mesh.VERTATTRIB.TANGENT, 4);
        attribToIntMapping.put(Mesh.VERTATTRIB.BITANGENT, 5);
        attribToIntMapping.put(Mesh.VERTATTRIB.MATERIAL, 6);
        attribToIntMapping.put(Mesh.VERTATTRIB.WEIGHTBIASESPERVERT, 7);
        attribToIntMapping.put(Mesh.VERTATTRIB.JOINTINDICESPERVERT, 8);
    }

    public List<Integer> vertAttributes;

    public Vertex(int point, int textureCoord, int normal) {
        vertAttributes = new ArrayList<>(3);
        setAttribute(point,POSITION);
        setAttribute(textureCoord,TEXTURE);
        setAttribute(normal,NORMAL);
    }

    public Vertex(int point) {
        vertAttributes = new ArrayList<>(1);
        setAttribute(point,POSITION);
    }

    public Vertex() {
        vertAttributes = new ArrayList<>();
    }

    public Vertex(List<Integer> vertAttributes) {
        this.vertAttributes = vertAttributes;
    }

    public void setAttribute(int val, int key) {
        if (key >= vertAttributes.size()) {
            for (int i = vertAttributes.size(); i < key + 1; i++) {
                vertAttributes.add(i, null);
            }
        }
        vertAttributes.set(key,val);
    }

    public Integer getAttribute(int key) {
        try {
            Integer val = vertAttributes.get(key);
            return val;
        }
        catch(Exception e) {
            return null;
        }
    }

    public Integer getAttribute(Mesh.VERTATTRIB key) {
        try {
            Integer val = vertAttributes.get(attribToIntMapping.get(key));
            return val;
        }
        catch(Exception e) {
            return null;
        }
    }

    public boolean isAttributePresent(int key) {
        try {
            vertAttributes.get(key);
            return vertAttributes.get(key) != null;
        }
        catch (Exception e){
            return false;
        }
    }

    public void display() {
        System.out.print("Vertex:: ");
        for (int i = 0; i < this.vertAttributes.size(); i++) {
            System.out.print(i+"="+vertAttributes.get(i)+" ");
        }
        System.out.println("]");
    }

    public String toString() {
        String ret = "";
        for(int i = 0;i<vertAttributes.size();i++) {
            ret += vertAttributes.get(i);
            if(i != vertAttributes.size() - 1) {
                ret += "::";
            }
        }
        return ret;
    }

    public boolean equals(Vertex v) {
        if(vertAttributes.size() != v.vertAttributes.size()) {
            return false;
        }

        boolean isEquals = true;
        for(int i = 0;i < vertAttributes.size();i++) {
            try {
                if (!this.vertAttributes.get(i).equals(v.vertAttributes.get(i))) {
                    isEquals = false;
                    break;
                }
            }catch(Exception e) {
                return false;
            }
        }
        return isEquals;
    }

}
