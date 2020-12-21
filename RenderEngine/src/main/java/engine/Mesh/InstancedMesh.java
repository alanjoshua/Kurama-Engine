package engine.Mesh;

import engine.Effects.Material;
import engine.Math.Vector;
import engine.geometry.MD5.MD5Utils;
import engine.geometry.MeshBuilderHints;
import engine.model.Model;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.glDrawArraysInstanced;
import static org.lwjgl.opengl.GL31.glDrawElementsInstanced;
import static org.lwjgl.opengl.GL33.glVertexAttribDivisor;


public class InstancedMesh extends Mesh {

    public static final int FLOAT_SIZE_BYTES = 4;
    public static final int VECTOR4F_SIZE_BYTES = 4 * FLOAT_SIZE_BYTES;
    public static final int MATRIX_SIZE_BYTES = 4 * InstancedMesh.VECTOR4F_SIZE_BYTES;
    public static final int MATRIX_SIZE_FLOATS = 16;
    public static final int INSTANCE_SIZE_BYTES = InstancedMesh.MATRIX_SIZE_BYTES + (2*VECTOR4F_SIZE_BYTES);
    public static final int INSTANCE_SIZE_FLOATS = InstancedMesh.MATRIX_SIZE_FLOATS + (2*4);

    public int numInstances;

    public int instanceDataVBO;
    public FloatBuffer instanceDataBuffer;

    public InstancedMesh(List<Integer> indices, List<Face> faces, List<List<Vector>> vertAttributes,
                         List<Material> materials, String meshLocation, MeshBuilderHints hints, int numInstances) {
        super(indices, faces, vertAttributes, materials, meshLocation, hints);
        this.numInstances = numInstances;
    }

    public InstancedMesh(Mesh mesh, int numInstances) {
        super(mesh.indices, mesh.faces, mesh.vertAttributes, mesh.materials, mesh.meshLocation, mesh.hints);
        this.numInstances = numInstances;
    }

    // Assumes all incoming models have shouldRender property be set to True
    public List<List<Model>> getRenderChunks(List<Model> models, Predicate<Model> filter) {
        List<List<Model>> chunks = new ArrayList<>();

        List<Model> currChunk = new ArrayList<>(numInstances);
        for(Model m: models) {
            if(filter.test(m)) {
                currChunk.add(m);
            }
            if(currChunk.size() >= numInstances) {
                chunks.add(currChunk);
                currChunk = new ArrayList<>();
            }
        }

        if(currChunk.size() != 0) {
            chunks.add(currChunk);
        }

        return chunks;
    }

    public List<List<Model>> getRenderChunks(List<Model> models) {
        List<List<Model>> chunks = new ArrayList<>();

        List<Model> currChunk = new ArrayList<>(numInstances);
        for(Model m: models) {
            currChunk.add(m);
            if(currChunk.size() >= numInstances) {
                chunks.add(currChunk);
                currChunk = new ArrayList<>();
            }
        }

        if(currChunk.size() != 0) {
            chunks.add(currChunk);
        }

        return chunks;
    }

    public void render(int numModels) {
        if(indices != null) {
            glDrawElementsInstanced(drawMode, indices.size(), GL_UNSIGNED_INT, 0, numModels);
        }
        else {
            glDrawArraysInstanced(drawMode, 0, getVertices().size(), numModels);
        }
    }

    @Override
    public void initOpenGLMeshData() {

        IntBuffer indicesBuffer;
        List<Integer> offsets = new ArrayList<>(vertAttributes.size());
        List<Integer> sizePerAttrib = new ArrayList<>(vertAttributes.size());
        int stride = 0;

        if(!isAttributePresent(Mesh.WEIGHTBIASESPERVERT)) {
            Vector negs = new Vector(MD5Utils.MAXWEIGHTSPERVERTEX, -1);
            List<Vector> att = new ArrayList<>(vertAttributes.get(Mesh.POSITION).size());
            att.add(negs);

            setAttribute(att, Mesh.WEIGHTBIASESPERVERT);
            setAttribute(att, Mesh.JOINTINDICESPERVERT);

            for(var f: faces) {
                for(var vert: f.vertices) {
                    vert.setAttribute(0, Vertex.WEIGHTBIASESPERVERT);
                    vert.setAttribute(0, Vertex.JOINTINDICESPERVERT);
                }
            }
        }

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

            int attribIndex = 0;  //Keeps track of vertex attribute index
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
                                t[j] = 0f;
                            }
                            tempBuffer.put(t);
                        }
                    }

                    tempBuffer.flip();

                    vboId = glGenBuffers();
                    vboIdList.add(vboId);
                    glBindBuffer(GL_ARRAY_BUFFER, vboId);
                    glBufferData(GL_ARRAY_BUFFER, tempBuffer, GL_STATIC_DRAW);
                    glEnableVertexAttribArray(attribIndex);
                    glVertexAttribPointer(attribIndex, sizePerAttrib.get(i) / sizeOfFloat, GL_FLOAT, false, 0, 0);

                    MemoryUtil.memFree(tempBuffer);   //Free buffer

                    attribIndex++;
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

//            Set up per instance vertex attributes such as transformation matrices

            // Model To World matrices
            int strideStart = 0;
            instanceDataVBO = glGenBuffers();
            vboIdList.add(instanceDataVBO);
            instanceDataBuffer = MemoryUtil.memAllocFloat(numInstances * INSTANCE_SIZE_FLOATS);
            glBindBuffer(GL_ARRAY_BUFFER, instanceDataVBO);
            for(int i = 0;i < 4; i++) {
                glVertexAttribPointer(attribIndex, 4, GL_FLOAT, false, INSTANCE_SIZE_BYTES, strideStart);
                glVertexAttribDivisor(attribIndex, 1);
                glEnableVertexAttribArray(attribIndex);
                attribIndex++;
                strideStart += VECTOR4F_SIZE_BYTES;
            }

            // Material global ind and atlas offset
            for(int i = 0;i < 2; i++) {
                glVertexAttribPointer(attribIndex, 4, GL_FLOAT, false, INSTANCE_SIZE_BYTES, strideStart);
                glVertexAttribDivisor(attribIndex, 1);
                glEnableVertexAttribArray(attribIndex);
                attribIndex++;
                strideStart += VECTOR4F_SIZE_BYTES;
            }

            // Texture offsets
//            glVertexAttribPointer(attribIndex, 2, GL_FLOAT, false, InstancedMesh.INSTANCE_SIZE_BYTES, strideStart);
//            glVertexAttribDivisor(attribIndex, 1);
//            glEnableVertexAttribArray(attribIndex);

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

}
