package models.DataStructure.Mesh;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

import Math.Vector;
import models.ModelBuilder;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

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

    public Mesh(List<List<Vector>> vertAttributes) {
        faces = new ArrayList<>(3);
    }

    public Vector getAttribute(int faceIndex, int vertIndex, int attributeIndex) {
        return vertAttributes.get(attributeIndex).get(faces.get(faceIndex).vertices.get(vertIndex).vertAttributes.get(attributeIndex));
    }

    public void addFace(Face f) {
        this.faces.add(f);
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

        if(isAttributePresent(Mesh.TEXTURE)) {
            System.out.println("Number of individual texture Coords: " + vertAttributes.get(1).size());
        }
        else {
            System.out.println("Number of individual texture Coords: 0");
        }

        if(isAttributePresent(Mesh.NORMAL)) {
            System.out.println("Number of individual normals: "+ vertAttributes.get(2).size());
        }
        else {
            System.out.println("Number of individual normals: 0");
        }

        System.out.println("Total number of individual lines drawn: "+lines);

    }

    //This functions assumes everything within it is stored as arrayLists
    public void trimEverything() {

        //First trim the vertex attribute lists
        for(List<Vector> l: vertAttributes) {
            if(l != null || l.size() > 0) {
                ((ArrayList<Vector>)l).trimToSize();
            }
            else {
                l = null;
            }
        }
        ((ArrayList<List<Vector>>)vertAttributes).trimToSize();

        for(Face f : faces) {
            for(Vertex v: f.vertices) {
                ((ArrayList<Integer>)v.vertAttributes).trimToSize();
            }
            ((ArrayList<Vertex>)f.vertices).trimToSize();
        }
        ((ArrayList<Face>)faces).trimToSize();

    }

    public List<Face> getFaces() {return faces;}

    public List<Vector> getAttributeList(int ind) {
        return vertAttributes.get(ind);
    }

    public boolean isAttributePresent(int key) {
        try {
            vertAttributes.get(key);
            return vertAttributes != null;
        }
        catch (Exception e){
            return false;
        }
    }

}
