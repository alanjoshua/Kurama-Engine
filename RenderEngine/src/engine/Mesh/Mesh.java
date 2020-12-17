package engine.Mesh;

import engine.Effects.Material;
import engine.Math.Vector;
import engine.geometry.MeshBuilderHints;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

public class Mesh {

    public static final int POSITION = 0;
    public static final int TEXTURE = 1;
    public static final int NORMAL = 2;
    public static final int COLOR = 3;
    public static final int TANGENT = 4;
    public static final int BITANGENT = 5;
    public static final int MATERIAL = 6;
    public static final int WEIGHTBIASESPERVERT = 7;
    public static final int JOINTINDICESPERVERT = 8;

    public List<Face> faces;
    public List<List<Vector>> vertAttributes;
    public List<Integer> indices;

    public String meshIdentifier=null;
    public String meshLocation;

    public int drawMode = GL_TRIANGLES;

    public boolean shouldCull = true;
    public int cullmode = GL_BACK;

    public int vaoId;
    public List<Integer> vboIdList;
    public List<Material> materials = new ArrayList<>();
    public MeshBuilderHints hints;
    public boolean isModified = false;

    public Mesh(List<Integer> indices, List<Face> faces, List<List<Vector>> vertAttributes, List<Material> materials,
                String meshLocation, MeshBuilderHints hints) {
        this.faces = faces;
        this.vertAttributes = vertAttributes;
        vboIdList = new ArrayList<>();
        this.indices = indices;
        this.meshLocation = meshLocation;
        this.hints = hints;

        if(materials == null) {
            this.materials.add(Material.DEFAULT_MATERIAL);
            List<Vector> matList = new ArrayList<>();
            matList.add(new Vector(new float[]{0}));
            setAttribute(matList,MATERIAL);
        }
        else {
            this.materials = materials;
        }
    }

    public void cleanUp() {
        try {
            for(int i = 0;i < vertAttributes.size();i++) {
                glDisableVertexAttribArray(i);
            }

            glBindBuffer(GL_ARRAY_BUFFER, 0);
            for (Integer vbo : vboIdList) {
                glDeleteBuffers(vbo);
            }

            for(Material material:materials) {
                material.texture.cleanUp();
            }

            glBindVertexArray(0);
            glDeleteVertexArrays(vaoId);
        }catch(Exception e) {
           //System.out.println("Couldn't clean OPENGL atrtibs of mesh: "+meshIdentifier);
        }
    }

    public void initToEndFullRender(int offset) {
        for(Material material:materials) {
            if (material.texture != null) {
                glActiveTexture(offset+GL_TEXTURE0);
                glBindTexture(GL_TEXTURE_2D, material.texture.getId());
            }

            if (material.normalMap != null) {
                glActiveTexture(offset+1+GL_TEXTURE0);
                glBindTexture(GL_TEXTURE_2D, material.normalMap.getId());
            }

            if (material.diffuseMap != null) {
                glActiveTexture(offset+2+GL_TEXTURE0);
                glBindTexture(GL_TEXTURE_2D, material.diffuseMap.getId());
            }

            if (material.specularMap != null) {
                glActiveTexture(offset+3+GL_TEXTURE0);
                glBindTexture(GL_TEXTURE_2D, material.specularMap.getId());
            }
            offset+=4;
        }
        glBindVertexArray(vaoId);
        render();
        endRender();
    }

    public void render() {
        if(indices != null) {
            glDrawElements(drawMode, indices.size(), GL_UNSIGNED_INT, 0);
        }
        else {
            glDrawArrays(drawMode, 0, getVertices().size());
        }
    }

    public void initRender() {
        glBindVertexArray(vaoId);
    }

    public void endRender() {
        glBindVertexArray(0);
//        int ind = 0;
//        for(Material material:materials) {
//            glBindTexture(GL_TEXTURE_2D,ind);
//            glBindTexture(GL_TEXTURE_2D,ind+1);
//            glBindTexture(GL_TEXTURE_2D,ind+2);
//            glBindTexture(GL_TEXTURE_2D,ind+3);
//            ind+=4;
//        }
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
                if(vertAttributes.get(i)!= null) {
                    for (int j = 0; j < vertAttributes.get(i).size(); j++) {
                        curr = vertAttributes.get(i).get(j);
                        if (curr != null) {
                            break;
                        }
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

        FloatBuffer colorBuffer = null;

        int vboId;

        try {

            vaoId = glGenVertexArrays();
            glBindVertexArray(vaoId);

            for(int i = 0;i < sizePerAttrib.size();i++) {
                if(vertAttributes.get(i)!=null) {

                    FloatBuffer tempBuffer = MemoryUtil.memAllocFloat(sizePerAttrib.get(i) * vertAttributes.get(i).size());
                    for (Vector v : vertAttributes.get(i)) {
                        if (v != null) {
                            tempBuffer.put(v.getData());
//                            v.display();
                        } else {    //Hack to handle nulls
                            float[] t = new float[sizePerAttrib.get(i) / sizeOfFloat];
                            for (int j = 0; j < sizePerAttrib.get(i) / sizeOfFloat; j++) {
//                                tempBuffer.put(0);
//                                System.out.println(0);
                                t[j] = 0f;
                            }
                            tempBuffer.put(t);
//                            new Vector(t).display();
                        }
                    }

                    tempBuffer.flip();

                    vboId = glGenBuffers();
                    vboIdList.add(vboId);
                    glBindBuffer(GL_ARRAY_BUFFER, vboId);
                    glBufferData(GL_ARRAY_BUFFER, tempBuffer, GL_STATIC_DRAW);
                    glEnableVertexAttribArray(i);
                    glVertexAttribPointer(i, sizePerAttrib.get(i) / sizeOfFloat, GL_FLOAT, false, 0, 0);

                    MemoryUtil.memFree(tempBuffer);   //Free buffer

                }
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

            glBindBuffer(GL_ARRAY_BUFFER,0);
            glBindVertexArray(0);

        }
        catch(Exception e) {
            System.out.println("caught exception here");
        }finally{
            if(colorBuffer != null) {
                MemoryUtil.memFree((colorBuffer));
            }

        }

    }

    public void setAttribute(List<Vector> val, int key) {
        if (key >= vertAttributes.size()) {
            for (int i = vertAttributes.size(); i < key + 1; i++) {
                vertAttributes.add(i, null);
            }
        }
        vertAttributes.set(key,val);
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
//    public void trimEverything() {
//
//        //First trim the vertex attribute lists
//        for(List<Vector> l: vertAttributes) {
//            if(l != null || l.size() > 0) {
//                ((ArrayList<Vector>)l).trimToSize();
//            }
//            else {
//                l = null;
//            }
//        }
//        ((ArrayList<List<Vector>>)vertAttributes).trimToSize();
//
//        for(Face f : faces) {
//            for(Vertex v: f.vertices) {
//                ((ArrayList<Integer>)v.vertAttributes).trimToSize();
//            }
//            ((ArrayList<Vertex>)f.vertices).trimToSize();
//        }
//        ((ArrayList<Face>)faces).trimToSize();
//
//    }

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

    public void deleteBuffers() {
        glDisableVertexAttribArray(0);

        // Delete the VBOs
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        for (int vboId : vboIdList) {
            glDeleteBuffers(vboId);
        }

        // Delete the VAO
        glBindVertexArray(0);
        glDeleteVertexArrays(vaoId);
    }

}
