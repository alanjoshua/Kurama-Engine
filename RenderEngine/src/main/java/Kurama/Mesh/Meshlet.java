package Kurama.Mesh;

import Kurama.Math.Vector;

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
    @Override
    public String toString() {
        return "primitive Count: " + primitiveCount + " vertex Count: "+ vertexCount
                + " index begin: "+ indexBegin + " vertex begin: " + vertexBegin
                + " objectId: " + objectId + " pos: "+ pos + " bound radius: " + boundRadius;
    }

}
