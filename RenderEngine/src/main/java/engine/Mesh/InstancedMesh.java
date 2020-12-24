package engine.Mesh;

import engine.Effects.Material;
import engine.Math.Vector;
import engine.geometry.MeshBuilderHints;
import engine.model.Model;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;


public class InstancedMesh extends Mesh {

    public int instanceChunkSize;

    public int instanceDataVBO;
    public FloatBuffer instanceDataBuffer;

    public InstancedMesh(List<Integer> indices, List<Face> faces, List<List<Vector>> vertAttributes,
                         List<Material> materials, String meshLocation, MeshBuilderHints hints, int instanceChunkSize) {
        super(indices, faces, vertAttributes, materials, meshLocation, hints);
        this.instanceChunkSize = instanceChunkSize;
    }

    public InstancedMesh(Mesh mesh, int instanceChunkSize) {
        super(mesh.indices, mesh.faces, mesh.vertAttributes, mesh.materials, mesh.meshLocation, mesh.hints);
        this.instanceChunkSize = instanceChunkSize;
        this.isAnimatedSkeleton = mesh.isAnimatedSkeleton;
    }

    // Assumes all incoming models have shouldRender property be set to True
    public List<List<Model>> getRenderChunks(List<Model> models, Predicate<Model> filter) {
        List<List<Model>> chunks = new ArrayList<>();

        List<Model> currChunk = new ArrayList<>(instanceChunkSize);
        for(Model m: models) {
            if(filter.test(m)) {
                currChunk.add(m);
            }
            if(currChunk.size() >= instanceChunkSize) {
                chunks.add(currChunk);
                currChunk = new ArrayList<>();
            }
        }

        if(currChunk.size() != 0) {
            chunks.add(currChunk);
        }

        return chunks;
    }

    public static List<List<Model>> getRenderChunks(List<Model> models, int chunkSize) {
        List<List<Model>> chunks = new ArrayList<>();

        List<Model> currChunk = new ArrayList<>(chunkSize);
        for(Model m: models) {
            currChunk.add(m);
            if(currChunk.size() >= chunkSize) {
                chunks.add(currChunk);
                currChunk = new ArrayList<>();
            }
        }

        if(currChunk.size() != 0) {
            chunks.add(currChunk);
        }

        return chunks;
    }

//    public void render(int numModels) {
//        if(indices != null) {
//            glDrawElementsInstanced(drawMode, indices.size(), GL_UNSIGNED_INT, 0, numModels);
//        }
//        else {
//            glDrawArraysInstanced(drawMode, 0, getVertices().size(), numModels);
//        }
//    }

//    @Override
//    public void initOpenGLMeshData() {
//
//        List<Vector> defaultVals = new ArrayList<>();
//        defaultVals.add(new Vector(0,0,0));
//        defaultVals.add(new Vector(2,0));
//        defaultVals.add(new Vector(0,0,0));
//        defaultVals.add(new Vector(0,0,0, 0));
//        defaultVals.add(new Vector(0,0,0));
//        defaultVals.add(new Vector(0,0,0));
//        defaultVals.add(new Vector(new float[]{0}));
//        defaultVals.add(new Vector(MD5Utils.MAXWEIGHTSPERVERTEX, -1));
//        defaultVals.add(new Vector(MD5Utils.MAXWEIGHTSPERVERTEX, -1));
//
//        IntBuffer indicesBuffer;
//        List<Integer> offsets = new ArrayList<>(vertAttributes.size());
//        List<Integer> sizePerAttrib = new ArrayList<>(vertAttributes.size());
//        int stride = 0;
//
//        if(!isAttributePresent(Mesh.WEIGHTBIASESPERVERT)) {
//            Vector negs = new Vector(MD5Utils.MAXWEIGHTSPERVERTEX, -1);
//            List<Vector> att = new ArrayList<>(vertAttributes.get(Mesh.POSITION).size());
//            att.add(negs);
//
//            setAttribute(att, Mesh.WEIGHTBIASESPERVERT);
//            setAttribute(att, Mesh.JOINTINDICESPERVERT);
//
//            for(var f: faces) {
//                for(var vert: f.vertices) {
//                    vert.setAttribute(0, Vertex.WEIGHTBIASESPERVERT);
//                    vert.setAttribute(0, Vertex.JOINTINDICESPERVERT);
//                }
//            }
//        }
//
//        final int sizeOfFloat = Float.SIZE / Byte.SIZE;
//
////        Calculate stride and offset
//        offsets.add(0);
//        try {
//        for(int i = 0;i < vertAttributes.size();i++) {
//            Vector curr = null;
//            int numberOfElements = 0;
//
//            if(curr == null) {
////                break;
//                if(vertAttributes.get(i)!= null) {
//                    for (int j = 0; j < vertAttributes.get(i).size(); j++) {
//                        curr = vertAttributes.get(i).get(j);
//                        if (curr != null) {
//                            if(curr.getNumberOfDimensions() != defaultVals.get(i).getNumberOfDimensions()) {
//                                throw new Exception("Dimensions do not match");
//                            }
//                            break;
//                        }
//                    }
//                }
//            }
//
//            if(curr == null) {
//                numberOfElements = defaultVals.get(i).getNumberOfDimensions();  //Assume a default of 4 if all positions are empty
//            }
//            else {
//                numberOfElements = curr.getNumberOfDimensions();
//            }
//
//            int size = numberOfElements * sizeOfFloat;
//            stride += size;
//            sizePerAttrib.add(size);
//            offsets.add(stride);
//        }
//        offsets.remove(offsets.size() - 1);
//
//        FloatBuffer colorBuffer = null;
//
//        int vboId;
//
//            int attribIndex = 0;  //Keeps track of vertex attribute index
//            vaoId = glGenVertexArrays();
//            glBindVertexArray(vaoId);
//
//            for(int i = 0;i < sizePerAttrib.size();i++) {
//                if(vertAttributes.get(i)!=null) {
//
//                    FloatBuffer tempBuffer = MemoryUtil.memAllocFloat(sizePerAttrib.get(i) * vertAttributes.get(i).size());
//                    for (Vector v : vertAttributes.get(i)) {
//                        if (v != null) {
//                            tempBuffer.put(v.getData());
//                        } else {    //Hack to handle nulls
//                            float[] t = defaultVals.get(i).getData();
//                            tempBuffer.put(defaultVals.get(i).getData());
//                        }
//                    }
//
//                    tempBuffer.flip();
//
//                    vboId = glGenBuffers();
//                    vboIdList.add(vboId);
//                    glBindBuffer(GL_ARRAY_BUFFER, vboId);
//                    glBufferData(GL_ARRAY_BUFFER, tempBuffer, GL_STATIC_DRAW);
//                    glEnableVertexAttribArray(attribIndex);
//                    glVertexAttribPointer(attribIndex, sizePerAttrib.get(i) / sizeOfFloat, GL_FLOAT, false, 0, 0);
//
//                    MemoryUtil.memFree(tempBuffer);   //Free buffer
//
//                    attribIndex++;
//                }
//            }
//
//            if(indices != null) {
//                indicesBuffer = MemoryUtil.memAllocInt(indices.size());
//                for(int i:indices) {
//                    indicesBuffer.put(i);
//                }
//                indicesBuffer.flip();
//
//                vboId = glGenBuffers();
//                vboIdList.add(vboId);
//                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboId);
//                glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);
//                MemoryUtil.memFree(indicesBuffer);  //Free buffer
//            }
//
////            Set up per instance vertex attributes such as transformation matrices
//
//            // Model To World matrices
//            int strideStart = 0;
//            instanceDataVBO = glGenBuffers();
//            vboIdList.add(instanceDataVBO);
//            instanceDataBuffer = MemoryUtil.memAllocFloat(instanceChunkSize * INSTANCE_SIZE_FLOATS);
//            glBindBuffer(GL_ARRAY_BUFFER, instanceDataVBO);
//            for(int i = 0;i < 4; i++) {
//                glVertexAttribPointer(attribIndex, 4, GL_FLOAT, false, INSTANCE_SIZE_BYTES, strideStart);
//                glVertexAttribDivisor(attribIndex, 1);
//                glEnableVertexAttribArray(attribIndex);
//                attribIndex++;
//                strideStart += VECTOR4F_SIZE_BYTES;
//            }
//
//            // Material global ind and atlas offset
//            for(int i = 0;i < 1; i++) {
//                glVertexAttribPointer(attribIndex, 4, GL_FLOAT, false, INSTANCE_SIZE_BYTES, strideStart);
//                glVertexAttribDivisor(attribIndex, 1);
//                glEnableVertexAttribArray(attribIndex);
//                attribIndex++;
//                strideStart += VECTOR4F_SIZE_BYTES;
//            }
//
//            glBindBuffer(GL_ARRAY_BUFFER,0);
//            glBindVertexArray(0);
//
//        }
//        catch(Exception e) {
//            System.out.println("caught exception here");
//            System.exit(1);
//        }finally{
//
//        }
//
//    }

}
