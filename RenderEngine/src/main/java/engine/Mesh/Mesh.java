package engine.Mesh;

import engine.Effects.Material;
import engine.Math.Vector;
import engine.geometry.MD5.MD5Utils;
import engine.geometry.MeshBuilderHints;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20C.glVertexAttribPointer;
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
    public boolean isAnimatedSkeleton = false;

    public Mesh(List<Integer> indices, List<Face> faces, List<List<Vector>> vertAttributes, List<Material> materials,
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

    public int initRender(int offset) {

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
        return offset;
    }

    public void initRender() {
        glBindVertexArray(vaoId);
    }

    public void endRender() {
        glBindVertexArray(0);
    }

    public void initOpenGLMeshData() {

        List<Vector> defaultVals = new ArrayList<>();
        defaultVals.add(new Vector(0,0,0));
        defaultVals.add(new Vector(2,0));
        defaultVals.add(new Vector(0,0,0));
        defaultVals.add(new Vector(0,0,0, 0));
        defaultVals.add(new Vector(0,0,0));
        defaultVals.add(new Vector(0,0,0));
        defaultVals.add(new Vector(new float[]{0}));
        defaultVals.add(new Vector(MD5Utils.MAXWEIGHTSPERVERTEX, -1));
        defaultVals.add(new Vector(MD5Utils.MAXWEIGHTSPERVERTEX, -1));

        if(!isAttributePresent(Mesh.WEIGHTBIASESPERVERT)) {
            Vector negs = new Vector(MD5Utils.MAXWEIGHTSPERVERTEX, -1);
            List<Vector> att = new ArrayList<>(vertAttributes.get(Mesh.POSITION).size());
            att.add(negs);
            for(int i = 0;i < indices.size(); i++) {
                att.add(negs);
            }
            setAttribute(att, Mesh.WEIGHTBIASESPERVERT);
            setAttribute(att, Mesh.JOINTINDICESPERVERT);

            for(var f: faces) {
                for(var vert: f.vertices) {
                    vert.setAttribute(0, Vertex.WEIGHTBIASESPERVERT);
                    vert.setAttribute(0, Vertex.JOINTINDICESPERVERT);
                }
            }
        }

        IntBuffer indicesBuffer = null;
        List<Integer> offsets = new ArrayList<>(vertAttributes.size());
        List<Integer> sizePerAttrib = new ArrayList<>(vertAttributes.size());
        int stride = 0;

        final int sizeOfFloat = Float.SIZE / Byte.SIZE;
        try {
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
                            if(curr.getNumberOfDimensions() != defaultVals.get(i).getNumberOfDimensions()) {
                                throw new Exception("Dimensions do not match");
                            }
                            break;
                        }
                    }
                }
            }

            if(curr == null) {
                numberOfElements = defaultVals.get(i).getNumberOfDimensions();  //Assume a default of 4 if all positions are empty
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
//                            float[] t = new float[sizePerAttrib.get(i) / sizeOfFloat];
                            float[] t = defaultVals.get(i).getData();
//                            for (int j = 0; j < t.length; j++) {
//                                tempBuffer.put(0);
//                                System.out.println(0);
//                                t[j] = 0f;
//                            }
                            tempBuffer.put(defaultVals.get(i).getData());
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

//        FloatBuffer posBuffer = null;
//        FloatBuffer textCoordsBuffer = null;
//        FloatBuffer vecNormalsBuffer = null;
//        FloatBuffer tangentsBuffer = null;
//        FloatBuffer bitangentBuffer = null;
//        FloatBuffer weightsBuffer = null;
//        FloatBuffer colorBuffer = null;
//        FloatBuffer jointIndicesBuffer = null;
//        IntBuffer indicesBuffer = null;
//        FloatBuffer materialIndBuffer = null;
//        final int sizeOfFloat = Float.SIZE / Byte.SIZE;
//        try {
//
//            vaoId = glGenVertexArrays();
//            glBindVertexArray(vaoId);
//
//            int vboId;
//
////            Position vectors
//            vboId = glGenBuffers();
//            vboIdList.add(vboId);
//            if(vertAttributes.get(Mesh.POSITION).get(0).getNumberOfDimensions() != 3) {
//                throw new Exception("Positions must be vec3");
//            }
//            posBuffer = MemoryUtil.memAllocFloat(vertAttributes.get(Mesh.POSITION).size() * 3 * sizeOfFloat);
//            for(Vector v: vertAttributes.get(Mesh.POSITION)) {
//                for(float val: v.getData()) {
//                    posBuffer.put(val);
//                }
//            }
//            posBuffer.flip();
//            glBindBuffer(GL_ARRAY_BUFFER, vboId);
//            glBufferData(GL_ARRAY_BUFFER, posBuffer, GL_STATIC_DRAW);
//            glEnableVertexAttribArray(0);
//            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
//            MemoryUtil.memFree(posBuffer);
//
////            Texture vectors
//            vboId = glGenBuffers();
//            vboIdList.add(vboId);
//            if(isAttributePresent(Mesh.TEXTURE)) {
//                textCoordsBuffer = MemoryUtil.memAllocFloat(vertAttributes.get(Mesh.TEXTURE).size() * 2 * sizeOfFloat);
//                for (Vector v : vertAttributes.get(Mesh.TEXTURE)) {
//                    if (v == null || v.getNumberOfDimensions() != 2) {
//                        textCoordsBuffer.put(0);
//                        textCoordsBuffer.put(0);
////                        throw new Exception("Textures must be vec2");
//                    }
//                    else {
//                        for (float val : v.getData()) {
//                            textCoordsBuffer.put(val);
//                        }
//                    }
//                }
//            }
//            else {
//                textCoordsBuffer = MemoryUtil.memAllocFloat(vertAttributes.get(0).size() * 2 * sizeOfFloat);
//            }
//            textCoordsBuffer.flip();
//            glBindBuffer(GL_ARRAY_BUFFER, vboId);
//            glBufferData(GL_ARRAY_BUFFER, textCoordsBuffer, GL_STATIC_DRAW);
//            glEnableVertexAttribArray(1);
//            glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
//            MemoryUtil.memFree(textCoordsBuffer);
//
////            Normals vectors
//            vboId = glGenBuffers();
//            vboIdList.add(vboId);
//            if(isAttributePresent(Mesh.NORMAL)) {
//                vecNormalsBuffer = MemoryUtil.memAllocFloat(vertAttributes.get(Mesh.NORMAL).size() * 3 * sizeOfFloat);
//                for (Vector v : vertAttributes.get(Mesh.NORMAL)) {
//                    if (v == null || v.getNumberOfDimensions() != 3) {
//                        vecNormalsBuffer.put(0);
//                        vecNormalsBuffer.put(0);
//                        vecNormalsBuffer.put(0);
////                        throw new Exception("Normals must be vec3");
//                    }
//                    else {
//                        for (float val : v.getData()) {
//                            vecNormalsBuffer.put(val);
//                        }
//                    }
//                }
//            }
//            else {
//                vecNormalsBuffer = MemoryUtil.memAllocFloat(vertAttributes.get(0).size() * 3 * sizeOfFloat);
//            }
//            vecNormalsBuffer.flip();
//            glBindBuffer(GL_ARRAY_BUFFER, vboId);
//            glBufferData(GL_ARRAY_BUFFER, vecNormalsBuffer, GL_STATIC_DRAW);
//            glEnableVertexAttribArray(2);
//            glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
//            MemoryUtil.memFree(vecNormalsBuffer);
//
//            //            Color vectors
//            vboId = glGenBuffers();
//            vboIdList.add(vboId);
//            if(isAttributePresent(Mesh.COLOR)) {
//                try {
//                    colorBuffer = MemoryUtil.memAllocFloat(vertAttributes.get(Mesh.COLOR).size() * 4 * sizeOfFloat);
//                }catch (NullPointerException e) {
//                    System.out.println(vertAttributes.get(Mesh.COLOR));
//                    e.printStackTrace();
//                }
//
//                for (Vector v : vertAttributes.get(Mesh.NORMAL)) {
//                    if (v== null || v.getNumberOfDimensions() != 4) {
//                        colorBuffer.put(0);
//                        colorBuffer.put(0);
//                        colorBuffer.put(0);
//                        colorBuffer.put(0);
////                        throw new Exception("Colors must be vec4");
//                    }
//                    else {
//                        for (float val : v.getData()) {
//                            colorBuffer.put(val);
//                        }
//                    }
//                }
//            }
//            else {
//                colorBuffer = MemoryUtil.memAllocFloat(vertAttributes.get(0).size() * 4 * sizeOfFloat);
//            }
//            colorBuffer.flip();
//            glBindBuffer(GL_ARRAY_BUFFER, vboId);
//            glBufferData(GL_ARRAY_BUFFER, colorBuffer, GL_STATIC_DRAW);
//            glEnableVertexAttribArray(3);
//            glVertexAttribPointer(3, 4, GL_FLOAT, false, 0, 0);
//            MemoryUtil.memFree(colorBuffer);
//
//            //            Tangent vectors
//            vboId = glGenBuffers();
//            vboIdList.add(vboId);
//            if(isAttributePresent(Mesh.TANGENT)) {
//                tangentsBuffer = MemoryUtil.memAllocFloat(vertAttributes.get(Mesh.TANGENT).size() * 3 * sizeOfFloat);
//
//                for (Vector v : vertAttributes.get(Mesh.TANGENT)) {
//                    if (v== null || v.getNumberOfDimensions() != 3) {
//                        tangentsBuffer.put(0);
//                        tangentsBuffer.put(0);
//                        tangentsBuffer.put(0);
////                        throw new Exception("Tangents must be vec3");
//                    }
//                    else {
//                        for (float val : v.getData()) {
//                            tangentsBuffer.put(val);
//                        }
//                    }
//                }
//            }
//            else {
//                tangentsBuffer = MemoryUtil.memAllocFloat(vertAttributes.get(0).size() * 3 * sizeOfFloat);
//            }
//            tangentsBuffer.flip();
//            glBindBuffer(GL_ARRAY_BUFFER, vboId);
//            glBufferData(GL_ARRAY_BUFFER, tangentsBuffer, GL_STATIC_DRAW);
//            glEnableVertexAttribArray(4);
//            glVertexAttribPointer(4, 3, GL_FLOAT, false, 0, 0);
//            MemoryUtil.memFree(tangentsBuffer);
//
//
//            //            Bi-Tangent vectors
//            vboId = glGenBuffers();
//            vboIdList.add(vboId);
//            if(isAttributePresent(Mesh.BITANGENT)) {
//
//                bitangentBuffer = MemoryUtil.memAllocFloat(vertAttributes.get(Mesh.BITANGENT).size() * 3 * sizeOfFloat);
//                for (Vector v : vertAttributes.get(Mesh.BITANGENT)) {
//                    if (v== null || v.getNumberOfDimensions() != 3) {
//                        bitangentBuffer.put(0);
//                        bitangentBuffer.put(0);
//                        bitangentBuffer.put(0);
//                    }
//                    else {
//                        for (float val : v.getData()) {
//                            bitangentBuffer.put(val);
//                        }
//                    }
//                }
//            }
//            else {
//                bitangentBuffer = MemoryUtil.memAllocFloat(vertAttributes.get(0).size() * sizeOfFloat * 3);
//            }
//            bitangentBuffer.flip();
//            glBindBuffer(GL_ARRAY_BUFFER, vboId);
//            glBufferData(GL_ARRAY_BUFFER, bitangentBuffer, GL_STATIC_DRAW);
//            glEnableVertexAttribArray(5);
//            glVertexAttribPointer(5, 3, GL_FLOAT, false, 0, 0);
//            MemoryUtil.memFree(bitangentBuffer);
//
//
//            //            Material indices
//            vboId = glGenBuffers();
//            vboIdList.add(vboId);
//            if(isAttributePresent(Mesh.MATERIAL)) {
//
//                 materialIndBuffer = MemoryUtil.memAllocFloat(vertAttributes.get(Mesh.MATERIAL).size() * sizeOfFloat);
//                for (Vector v : vertAttributes.get(Mesh.MATERIAL)) {
//                    if (v== null || v.getNumberOfDimensions() != 1) {
//                        materialIndBuffer.put(0);
//                    }
//                    else {
//                        for (float val : v.getData()) {
//                            materialIndBuffer.put(val);
//                        }
//                    }
//                }
//            }
//            else {
//                materialIndBuffer = MemoryUtil.memAllocFloat(vertAttributes.get(0).size() * sizeOfFloat);
//            }
//            materialIndBuffer.flip();
//            glBindBuffer(GL_ARRAY_BUFFER, vboId);
//            glBufferData(GL_ARRAY_BUFFER, materialIndBuffer, GL_STATIC_DRAW);
//            glEnableVertexAttribArray(6);
//            glVertexAttribPointer(6, 1, GL_FLOAT, false, 0, 0);
//            MemoryUtil.memFree(materialIndBuffer);
//
//            //            Joint weight vectors
//            vboId = glGenBuffers();
//            vboIdList.add(vboId);
//            if(isAttributePresent(Mesh.WEIGHTBIASESPERVERT)) {
//
//                weightsBuffer = MemoryUtil.memAllocFloat(vertAttributes.get(Mesh.WEIGHTBIASESPERVERT).size() * 4 * sizeOfFloat);
//                for (Vector v : vertAttributes.get(Mesh.WEIGHTBIASESPERVERT)) {
//                    if (v== null || v.getNumberOfDimensions() != 4) {
//                        weightsBuffer.put(-1);
//                        weightsBuffer.put(-1);
//                        weightsBuffer.put(-1);
//                        weightsBuffer.put(-1);
//                    }
//                    else {
//                        for (float val : v.getData()) {
//                            weightsBuffer.put(val);
//                        }
//                    }
//                }
//            }
//            else {
//                weightsBuffer = MemoryUtil.memAllocFloat(vertAttributes.get(0).size() * 4 * sizeOfFloat);
//                for(var ind: vertAttributes.get(0)) {
//                    weightsBuffer.put(-1);
//                    weightsBuffer.put(-1);
//                    weightsBuffer.put(-1);
//                    weightsBuffer.put(-1);
//                }
//            }
//            weightsBuffer.flip();
//            glBindBuffer(GL_ARRAY_BUFFER, vboId);
//            glBufferData(GL_ARRAY_BUFFER, weightsBuffer, GL_STATIC_DRAW);
//            glEnableVertexAttribArray(7);
//            glVertexAttribPointer(7, 4, GL_FLOAT, false, 0, 0);
//            MemoryUtil.memFree(weightsBuffer);
//
//            //            Joint indices
//            vboId = glGenBuffers();
//            vboIdList.add(vboId);
//            if(isAttributePresent(Mesh.JOINTINDICESPERVERT)) {
//
//                jointIndicesBuffer = MemoryUtil.memAllocFloat(vertAttributes.get(Mesh.JOINTINDICESPERVERT).size() * 4 * sizeOfFloat);
//                for (Vector v : vertAttributes.get(Mesh.JOINTINDICESPERVERT)) {
//                    if (v== null || v.getNumberOfDimensions() != 4) {
//                        jointIndicesBuffer.put(-1);
//                        jointIndicesBuffer.put(-1);
//                        jointIndicesBuffer.put(-1);
//                        jointIndicesBuffer.put(-1);
//                    }
//                    else {
//                        for (float val : v.getData()) {
//                            jointIndicesBuffer.put(val);
//                        }
//                    }
//                }
//            }
//            else {
//                jointIndicesBuffer = MemoryUtil.memAllocFloat(vertAttributes.get(0).size() * 4 * sizeOfFloat);
//                for(var ind: vertAttributes.get(0)) {
//                    jointIndicesBuffer.put(-1);
//                    jointIndicesBuffer.put(-1);
//                    jointIndicesBuffer.put(-1);
//                    jointIndicesBuffer.put(-1);
//                }
//            }
//            jointIndicesBuffer.flip();
//            glBindBuffer(GL_ARRAY_BUFFER, vboId);
//            glBufferData(GL_ARRAY_BUFFER, jointIndicesBuffer, GL_STATIC_DRAW);
//            glEnableVertexAttribArray(8);
//            glVertexAttribPointer(8, 4, GL_FLOAT, false, 0, 0);
//            MemoryUtil.memFree(jointIndicesBuffer);

//            INDEX BUFFER
//        IntBuffer indicesBuffer=null;
            if(indices != null) {
//                int vboId;
                indicesBuffer = MemoryUtil.memAllocInt(indices.size());
                for(int i:indices) {
                    indicesBuffer.put(i);
                }
                indicesBuffer.flip();

                vboId = glGenBuffers();
                vboIdList.add(vboId);
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboId);
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);
            }

            glBindBuffer(GL_ARRAY_BUFFER,0);
            glBindVertexArray(0);
            MemoryUtil.memFree(indicesBuffer);
        }
        catch(Exception e) {
            System.out.println("caught exception here");
            e.printStackTrace();
            System.exit(1);
        }finally{
//            if (posBuffer != null) {
//                MemoryUtil.memFree(posBuffer);
//            }
//            if (textCoordsBuffer != null) {
//                MemoryUtil.memFree(textCoordsBuffer);
//            }
//            if (vecNormalsBuffer != null) {
//                MemoryUtil.memFree(vecNormalsBuffer);
//            }
//            if (materialIndBuffer != null) {
//                MemoryUtil.memFree(materialIndBuffer);
//            }
//            if (tangentsBuffer != null) {
//                MemoryUtil.memFree(vecNormalsBuffer);
//            }
//            if (bitangentBuffer != null) {
//                MemoryUtil.memFree(vecNormalsBuffer);
//            }
//            if (weightsBuffer != null) {
//                MemoryUtil.memFree(weightsBuffer);
//            }
//            if (jointIndicesBuffer != null) {
//                MemoryUtil.memFree(jointIndicesBuffer);
//            }
//            if (indicesBuffer != null) {
//                MemoryUtil.memFree(indicesBuffer);
//            }
//            if(colorBuffer != null) {
//                MemoryUtil.memFree((colorBuffer));
//            }

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
            var ans = vertAttributes.get(key);
            return (ans != null);
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
