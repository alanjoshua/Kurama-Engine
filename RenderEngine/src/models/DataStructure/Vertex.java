package models.DataStructure;

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
        vertAttributes.add(POSITION,point);
        vertAttributes.add(TEXTURE,textureCoord);
        vertAttributes.add(NORMAL,normal);
    }

    public Vertex(int point) {
        vertAttributes = new ArrayList<>(1);
        vertAttributes.add(POSITION,point);
    }

    public Vertex(List<Integer> vertAttributes) {
        this.vertAttributes = vertAttributes;
    }

}
