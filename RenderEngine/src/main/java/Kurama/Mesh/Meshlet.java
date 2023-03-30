package Kurama.Mesh;

import Kurama.Math.Vector;

import java.util.ArrayList;
import java.util.List;

public class Meshlet {
    public int primitiveCount;
    public int vertexCount;
    public int indexBegin;
    public int vertexBegin;

    public Vector pos;
    public float boundRadius;
    public int objectId;
    public float density = 0;
    public int treeDepth = 0;
    public Meshlet parent = null;
    public List<Meshlet> children = new ArrayList<>();

    // Used only while creating the hierarchy structure, destroyed later
    public List<Integer> vertIndices;

    @Override
    public String toString() {
        return "primitive Count: " + primitiveCount + " vertex Count: "+ vertexCount
                + " index begin: "+ indexBegin + " vertex begin: " + vertexBegin
                + " objectId: " + objectId + " pos: "+ pos + " bound radius: " + boundRadius;
    }

}
