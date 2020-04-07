package models.DataStructure.Mesh;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

import Math.Vector;
import models.DataStructure.Texture;
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
    public List<Integer> indices;

    public int vaoId;
    public List<Integer> vboIdList;

    public Texture texture;

    public Mesh(List<Face> faces, List<List<Vector>> vertAttributes) {
        this.faces = faces;
        this.vertAttributes = vertAttributes;
    }

    public Mesh(List<Integer> indices, List<Face> faces, List<List<Vector>> vertAttributes) {
        this.faces = faces;
        this.vertAttributes = vertAttributes;
        vboIdList = new ArrayList<>();
        this.indices = indices;
    }

    public void cleanUp() {
        try {
            glDisableVertexAttribArray(0);
            glDisableVertexAttribArray(1);

            glBindBuffer(GL_ARRAY_BUFFER, 0);
            for (Integer vbo : vboIdList) {
                glDeleteBuffers(vbo);
            }

            texture.cleanUp();

            glBindVertexArray(0);
            glDeleteVertexArrays(vaoId);
        }catch(Exception e) {
            System.err.println("Couldn't clean mesh. OpenGL bindings might be missing");
        }
    }

    public void render() {
        try {

            if(texture != null) {
//              Activate first texture
                glActiveTexture(GL_TEXTURE0);
//                Bind texture
                glBindTexture(GL_TEXTURE_2D, texture.getId());
            }

            glBindVertexArray(vaoId);

            if(indices != null) {
                glDrawElements(GL_TRIANGLES, indices.size(), GL_UNSIGNED_INT, 0);
            }
            else {
                glDrawArrays(GL_TRIANGLES, 0, getVertices().size());
            }

            glBindVertexArray(0);
        }catch(Exception e) {
            System.err.println("Couldn't render mesh. OpenGL bindings might be missing");
        }
    }

    public void initOpenGLMeshData() {

        IntBuffer indicesBuffer = null;
        List<Integer> offsets = new ArrayList<>(vertAttributes.size());
        List<Integer> sizePerAttrib = new ArrayList<>(vertAttributes.size());
        int stride = 0;

        final int sizeOfFloat = Float.SIZE / Byte.SIZE;

//        Calculate stride and offset
        offsets.add(0);
        for(int i = 0;i < vertAttributes.size();i++) {
            Vector curr = null;
            int numberOfElements = 0;

            if(curr == null) {
//                break;
                for(int j = 0;j < vertAttributes.get(i).size();j++) {
                    curr = vertAttributes.get(i).get(j);
                    if(curr != null) {
                        break;
                    }
                }
            }

            if(curr == null) {
                numberOfElements = 4;  //Assume a default of 4 if all positions are empty
            }
            else {
                numberOfElements = curr.getNumberOfDimensions();
            }

            int size = numberOfElements * sizeOfFloat;
            stride += size;
            sizePerAttrib.add(size);
            offsets.add(stride);
        }
        offsets.remove(offsets.size() - 1);

        FloatBuffer verticesBuffer = null;
        FloatBuffer colorBuffer = null;

        int vboId;

        try {

            vaoId = glGenVertexArrays();
            glBindVertexArray(vaoId);

            for(int i = 0;i < sizePerAttrib.size();i++) {
                FloatBuffer tempBuffer = MemoryUtil.memAllocFloat(sizePerAttrib.get(i) * vertAttributes.get(i).size());
                for(Vector v: vertAttributes.get(i)) {
                    if(v != null) {
                        tempBuffer.put(v.getData());
                    }
                    else {    //Hack to handle nulls
                        for(int j = 0;j < sizePerAttrib.get(i)/sizeOfFloat;j++) {
                            tempBuffer.put(0);
                        }
                    }
                }
                tempBuffer.flip();

                vboId = glGenBuffers();
                vboIdList.add(vboId);
                glBindBuffer(GL_ARRAY_BUFFER, vboId);
                glBufferData(GL_ARRAY_BUFFER,tempBuffer,GL_STATIC_DRAW);
                glEnableVertexAttribArray(i);
                glVertexAttribPointer(i,sizePerAttrib.get(i)/sizeOfFloat,GL_FLOAT,false,0,0);

                MemoryUtil.memFree(tempBuffer);   //Free buffer

            }

            if(indices != null) {
                indicesBuffer = MemoryUtil.memAllocInt(indices.size());
                for(int i:indices) {
                    indicesBuffer.put(i);
                }
                indicesBuffer.flip();

                vboId = glGenBuffers();
                vboIdList.add(vboId);
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboId);
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);

                MemoryUtil.memFree(indicesBuffer);  //Free buffer
            }

//            glBindBuffer(GL_ARRAY_BUFFER,0);
//            glBindVertexArray(0);

            Random rand = new Random();

            colorBuffer = MemoryUtil.memAllocFloat(getVertices().size() * 3);
            for(Vector v: getVertices()) {
                float[] color = new float[] {rand.nextFloat(),rand.nextFloat(),rand.nextFloat()};
//                float[] color = new float[] {0.2f,0.2f,0.2f};
                for(float val:color) {
                    colorBuffer.put(val);
                }
            }
            colorBuffer.flip();

            vboId = glGenBuffers();
            vboIdList.add(vboId);
            glBindBuffer(GL_ARRAY_BUFFER,vboId);
            glBufferData(GL_ARRAY_BUFFER,colorBuffer,GL_STATIC_DRAW);
            glEnableVertexAttribArray(3);
            glVertexAttribPointer(3,3,GL_FLOAT,false,0,0);

            glBindBuffer(GL_ARRAY_BUFFER,0);
            glBindVertexArray(0);

        }finally  {
            if(colorBuffer != null) {
                MemoryUtil.memFree((colorBuffer));
            }

        }

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
