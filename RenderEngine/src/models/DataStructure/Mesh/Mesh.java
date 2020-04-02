package models.DataStructure.Mesh;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

import Math.Vector;
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

    public int vaoId;
    public int coordVboId;
    public int colorVboId;
    public int indexVboId;
    public int vertexCount;

    public Mesh(List<Face> faces, List<List<Vector>> vertAttributes) {
        this.faces = faces;
        this.vertAttributes = vertAttributes;
    }

    public void initOpenGLMeshData() {
        FloatBuffer verticesBuffer = null;
        IntBuffer indicesBuffer = null;
        FloatBuffer colorBuffer = null;

        try {
//          Calculate vertice Buffer
            verticesBuffer = MemoryUtil.memAllocFloat(getVertices().size() * 4);
            for(Vector v: getVertices()) {
                for(float val: v.getData()) {
                    verticesBuffer.put(val);
                }
            }
            verticesBuffer.flip();

//            Calculate index Buffer
            vertexCount = 0;
            for(Face f:faces) {
                for(Vertex v: f.vertices) {
                    vertexCount++;
                }
            }
            indicesBuffer = MemoryUtil.memAllocInt(vertexCount);
            for(Face f:faces) {
                for(Vertex v: f.vertices) {
                   indicesBuffer.put(v.getAttribute(Vertex.POSITION));
                }
            }
            indicesBuffer.flip();

            Random rand = new Random();

            colorBuffer = MemoryUtil.memAllocFloat(getVertices().size() * 3);
            for(Vector v: getVertices()) {
                float[] color = new float[] {rand.nextFloat(),rand.nextFloat(),rand.nextFloat()};
//                float[] color = new float[] {1f,1f,1f};
                for(float val:color) {
                    colorBuffer.put(val);
                }
            }
            colorBuffer.flip();

             vaoId = glGenVertexArrays();
             glBindVertexArray(vaoId);

             coordVboId = glGenBuffers();
             glBindBuffer(GL_ARRAY_BUFFER, coordVboId);
             glBufferData(GL_ARRAY_BUFFER,verticesBuffer,GL_STATIC_DRAW);
             glEnableVertexAttribArray(0);
             glVertexAttribPointer(0,4,GL_FLOAT,false,0,0);

             colorVboId = glGenBuffers();
             glBindBuffer(GL_ARRAY_BUFFER,colorVboId);
             glBufferData(GL_ARRAY_BUFFER,colorBuffer,GL_STATIC_DRAW);
             glEnableVertexAttribArray(1);
             glVertexAttribPointer(1,3,GL_FLOAT,false,0,0);

            indexVboId = glGenBuffers();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,indexVboId);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER,indicesBuffer,GL_STATIC_DRAW);

             glBindBuffer(GL_ARRAY_BUFFER,0);
             glBindVertexArray(0);

        }finally  {
            if(verticesBuffer != null) {
                MemoryUtil.memFree((verticesBuffer));
            }
            if(indicesBuffer != null) {
                MemoryUtil.memFree((indicesBuffer));
            }
            if(colorBuffer != null) {
                MemoryUtil.memFree((colorBuffer));
            }
        }

    }

    public void cleanUp() {
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER,0);
        glDeleteBuffers(coordVboId);
        glDeleteBuffers(indexVboId);
        glDeleteBuffers(colorVboId);

        glBindVertexArray(0);
        glDeleteVertexArrays(vaoId);
    }

    public void render() {
        glBindVertexArray(vaoId);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);

        glDrawElements(GL_TRIANGLES,vertexCount,GL_UNSIGNED_INT,0);

        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);

        glBindVertexArray(0);
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
