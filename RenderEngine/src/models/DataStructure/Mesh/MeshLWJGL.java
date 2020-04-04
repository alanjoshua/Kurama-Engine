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

public class MeshLWJGL {

    public static final int POSITION = 0;
    public static final int TEXTURE = 1;
    public static final int NORMAL = 2;

    public List<Integer> indices;
    public List<List<Vector>> vertAttributes;

    public int vaoId;
    public List<Integer> vboIdList;
    public int vertexCount;

    public MeshLWJGL(List<Integer> indices, List<List<Vector>> vertAttributes) {
        vboIdList = new ArrayList<>();
        this.indices = indices;
        this.vertAttributes = vertAttributes;
    }

    public void initOpenGLMeshData() {
        FloatBuffer verticesBuffer = null;
        IntBuffer indicesBuffer = null;
        FloatBuffer colorBuffer = null;

        int vboId;

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
            vertexCount = indices.size();

            indicesBuffer = MemoryUtil.memAllocInt(vertexCount);
            for(int i:indices) {
                indicesBuffer.put(i);
            }
            indicesBuffer.flip();

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

            vaoId = glGenVertexArrays();
            glBindVertexArray(vaoId);

            vboId = glGenBuffers();
            vboIdList.add(vboId);
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER,verticesBuffer,GL_STATIC_DRAW);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0,4,GL_FLOAT,false,0,0);

            vboId = glGenBuffers();
            vboIdList.add(vboId);
            glBindBuffer(GL_ARRAY_BUFFER,vboId);
            glBufferData(GL_ARRAY_BUFFER,colorBuffer,GL_STATIC_DRAW);
            glEnableVertexAttribArray(1);
            glVertexAttribPointer(1,3,GL_FLOAT,false,0,0);

            vboId = glGenBuffers();
            vboIdList.add(vboId);
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,vboId);
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
        for(Integer vbo:vboIdList) {
            glDeleteBuffers(vbo);
        }

        glBindVertexArray(0);
        glDeleteVertexArrays(vaoId);
    }

    public void render() {
        glBindVertexArray(vaoId);

        glDrawElements(GL_TRIANGLES,vertexCount,GL_UNSIGNED_INT,0);

        glBindVertexArray(0);
    }

    public List<Vector> getVertices() {
        return vertAttributes.get(POSITION);
    }

    public void displayMeshInformation() {

//        HashMap<Integer, Integer> data = new HashMap<>();
//        for(Face f: faces) {
//            Integer curr = data.get(f.size());
//            if(curr == null) {curr = 0;}
//            data.put(f.size(),curr+1);
//        }
//        System.out.println("Vertex Info: ");
//        int lines = 0;
//        for(int key: data.keySet()) {
//            System.out.println("Number of "+key+" sided polygons: "+data.get(key));
//            lines += key * data.get(key);
//        }
//        System.out.println("Number of individual 3D points: "+ vertAttributes.get(0).size());
//
//        if(isAttributePresent(Mesh.TEXTURE)) {
//            System.out.println("Number of individual texture Coords: " + vertAttributes.get(1).size());
//        }
//        else {
//            System.out.println("Number of individual texture Coords: 0");
//        }
//
//        if(isAttributePresent(Mesh.NORMAL)) {
//            System.out.println("Number of individual normals: "+ vertAttributes.get(2).size());
//        }
//        else {
//            System.out.println("Number of individual normals: 0");
//        }
//
//        System.out.println("Total number of individual lines drawn: "+lines);

    }

}
