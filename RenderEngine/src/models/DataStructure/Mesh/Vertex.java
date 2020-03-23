package models.DataStructure.Mesh;

import Math.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Vertex {

    public static final int POSITION = 0;
    public static final int TEXTURE = 1;
    public static final int NORMAL = 2;

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
        if(key < vertAttributes.size()) {
            vertAttributes.set(key,val);
        }
        else {
            for(int i = vertAttributes.size();i < key+1;i++) {
                vertAttributes.add(i,null);
            }
            vertAttributes.set(key, val);
        }
    }

    public boolean isAttributePresent(int key) {
        try {
            vertAttributes.get(key);
            if(vertAttributes.get(key) != null) {
                return true;
            }
            else {
                return false;
            }
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

}
