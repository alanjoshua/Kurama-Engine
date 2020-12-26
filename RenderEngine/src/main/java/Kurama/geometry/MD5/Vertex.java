package Kurama.geometry.MD5;

import Kurama.Math.Vector;

public class Vertex {

    public int index;
    public Vector texCoords;
    public int startWeight;
    public int countWeight;

    public Vertex(int index, Vector texCoords, int startWeight, int countWeight) {
        this.index = index;
        this.texCoords = texCoords;
        this.startWeight = startWeight;
        this.countWeight = countWeight;
    }

}
