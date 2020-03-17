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

    public void displayMeshInformation() {
        HashMap<Integer, Integer> data = new HashMap<>();
        for(Face f: faces) {
            Integer curr = data.get(f.size());
            if(curr == null) {curr = 0;}
            data.put(f.size(),curr+1);
        }
        System.out.println("Vertex Info: ");
        int lines = 0;
        for(int key: data.keySet()) {
            System.out.println("Number of "+key+" sided polygons: "+data.get(key));
            lines += key * data.get(key);
        }
        System.out.println("Number of individual 3D points: "+ vertAttributes.get(0).size());
        System.out.println("Number of individual texture Coords: "+ vertAttributes.get(1).size());
        System.out.println("Number of individual normals: "+ vertAttributes.get(2).size());
        System.out.println("Total number of individual lines drawn: "+lines);

    }

    public List<Face> getFaces() {return faces;}

    public List<Vector> getAttributeList(int ind) {
        return vertAttributes.get(ind);
    }

    public boolean isAttributePresent(int key) {
        try {
            vertAttributes.get(key);
            if(vertAttributes == null) {
                return false;
            }
            return true;
        }
        catch (Exception e){
            return false;
        }
    }

}
