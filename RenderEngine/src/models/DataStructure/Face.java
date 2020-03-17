package models.DataStructure;

import java.util.ArrayList;
import java.util.List;

public class Face {

    public List<Vertex> vertices;

    public Face() {
        vertices = new ArrayList<>(2);
    }

    public Face(List<Vertex> vertices) {
        this.vertices = vertices;
    }

    public int size() {return vertices.size();}

    public int get(int vertInd, int attribute) {
        return vertices.get(vertInd).vertAttributes.get(attribute);
    }

    public int get(int vertInd) {
        return vertices.get(vertInd).vertAttributes.get(Vertex.POSITION);
    }
}
