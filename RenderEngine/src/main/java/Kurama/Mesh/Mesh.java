package Kurama.Mesh;

import Kurama.Math.Vector;
import Kurama.geometry.MeshBuilderHints;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static Kurama.Mesh.Mesh.VERTATTRIB.NORMAL;
import static Kurama.Mesh.Mesh.VERTATTRIB.TEXTURE;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;

public class Mesh {

    public enum VERTATTRIB {
        POSITION("pos"), TEXTURE("tex"), NORMAL("norm"), COLOR("color"),
        TANGENT("tan"), BITANGENT("bitan"), MATERIAL("mat"),
        WEIGHTBIASESPERVERT("weight"), JOINTINDICESPERVERT("jointInd");

        public final String label;
        private VERTATTRIB(String label) {
            this.label = label;
        }
    }
    // from Legacy reasons
    public static Map<Integer, VERTATTRIB> attribMapping = new HashMap<>();
    {
        attribMapping.put(0, VERTATTRIB.POSITION);
        attribMapping.put(1, TEXTURE);
        attribMapping.put(2, NORMAL);
        attribMapping.put(3, VERTATTRIB.COLOR);
        attribMapping.put(4, VERTATTRIB.TANGENT);
        attribMapping.put(5, VERTATTRIB.BITANGENT);
        attribMapping.put(6, VERTATTRIB.MATERIAL);
        attribMapping.put(7, VERTATTRIB.WEIGHTBIASESPERVERT);
        attribMapping.put(8, VERTATTRIB.JOINTINDICESPERVERT);
    }

    public List<Face> faces;
    public Map<VERTATTRIB, List<Vector>> vertAttributes;
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
    public boolean isAnimatedSkeleton = false;
    public boolean isInstanced = false;
    public float boundingRadius = 1;

    public int instanceChunkSize;
    public int instanceDataVBO;
    public FloatBuffer instanceDataBuffer;

    public Mesh(List<Integer> indices, List<Face> faces, Map<VERTATTRIB, List<Vector>> vertAttributes, List<Material> materials,
                String meshLocation, MeshBuilderHints hints) {
        this.faces = faces;
        this.vertAttributes = vertAttributes;
        vboIdList = new ArrayList<>();
        this.indices = indices;
        this.meshLocation = meshLocation;
        this.hints = hints;

        if(materials == null) {
            this.materials.add(new Material());
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

    public void cleanUp(boolean shouldCleanMaterials) {
        try {
            for(int i = 0;i < vertAttributes.size();i++) {
                glDisableVertexAttribArray(i);
            }

            glBindBuffer(GL_ARRAY_BUFFER, 0);
            for (Integer vbo : vboIdList) {
                glDeleteBuffers(vbo);
            }

            if(shouldCleanMaterials) {
                for (Material material : materials) {
                    material.texture.cleanUp();
                }
            }

            glBindVertexArray(0);
            glDeleteVertexArrays(vaoId);
        }catch(Exception e) {
            //System.out.println("Couldn't clean OPENGL atrtibs of mesh: "+meshIdentifier);
        }
    }

    public void setAttribute(List<Vector> val, VERTATTRIB key) {
        if(val != null) {
            vertAttributes.put(key, val);
        }
    }

    public Vector getAttribute(int faceIndex, int vertIndex, int attributeIndex) {
        return vertAttributes.get(attributeIndex).get(faces.get(faceIndex).vertices.get(vertIndex).vertAttributes.get(attributeIndex));
    }

    public void addFace(Face f) {
        this.faces.add(f);
    }

    public List<Vector> getVertices() {
        return vertAttributes.get(VERTATTRIB.POSITION);
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

        if(isAttributePresent(TEXTURE)) {
            System.out.println("Number of individual texture Coords: " + vertAttributes.get(1).size());
        }
        else {
            System.out.println("Number of individual texture Coords: 0");
        }

        if(isAttributePresent(NORMAL)) {
            System.out.println("Number of individual normals: "+ vertAttributes.get(2).size());
        }
        else {
            System.out.println("Number of individual normals: 0");
        }

        System.out.println("Total number of individual lines drawn: "+lines);

    }

    public List<Vector> getAttributeList(VERTATTRIB ind) {
        return vertAttributes.get(ind);
    }

    public boolean isAttributePresent(VERTATTRIB key) {
        return vertAttributes.containsKey(key);
    }

}
