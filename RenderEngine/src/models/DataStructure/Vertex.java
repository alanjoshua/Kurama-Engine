package models.DataStructure;

import Math.Vector;

public class Vertex {

    public Vector point;
    public Vector textureCoord;
    public Vector normal;

    public Vertex(Vector point, Vector textureCoord, Vector normal) {
        this.point = point;
        this.textureCoord = textureCoord;
        this.normal = normal;
    }

}
