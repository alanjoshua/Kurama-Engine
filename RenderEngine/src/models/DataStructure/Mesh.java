package models.DataStructure;

import java.util.*;
import java.util.stream.Collectors;

import Math.Vector;

public class Mesh {

    public static final int POSITION = 0;
    public static final int TEXTURE = 1;
    public static final int NORMAL = 2;

    public List<Face> faces;
    public List<List<Vector>> vertAttributes;

    public Mesh(List<Face> faces, List<List<Vector>> vertAttributes) {
        this.faces = faces;
        this.vertAttributes = vertAttributes;
    }

    public Vector getAttribute(int faceIndex, int vertIndex, int attributeIndex) {
        return vertAttributes.get(attributeIndex).get(faces.get(faceIndex).vertices.get(vertIndex).vertAttributes.get(attributeIndex));
    }

    public List<Vector> getVertices() {
        return vertAttributes.get(POSITION);
    }

    public List<Face> getFaces() {return faces;}

    public List<Vector> getAttributeList(int ind) {
        return vertAttributes.get(ind);
    }

}
