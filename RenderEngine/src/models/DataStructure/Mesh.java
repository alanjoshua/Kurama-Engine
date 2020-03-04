package models.DataStructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import Math.Vector;

public class Mesh {

    public List<Face> faces;
    private List<Vector> vertices;

    public Mesh(List<Face> faces) {
        this.faces = faces;
        this.vertices = calculateVerticesFromMesh();
    }

    public List<Vector> getVertices() {
        return vertices;
    }

    public List<Vector> calculateVerticesFromMesh() {
        List<Vector> verts = new ArrayList<>();
        for(Face f: faces) {
            for(Vertex v : f.vertices) {
                verts.add(v.point);
            }
        }
        return verts;
    }

}
