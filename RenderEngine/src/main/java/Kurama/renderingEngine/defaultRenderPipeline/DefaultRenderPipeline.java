package Kurama.renderingEngine.defaultRenderPipeline;

import Kurama.Effects.Material;
import Kurama.Math.Vector;
import Kurama.Mesh.InstancedMesh;
import Kurama.Mesh.Mesh;
import Kurama.Mesh.Vertex;
import Kurama.game.Game;
import Kurama.geometry.MD5.MD5Utils;
import Kurama.renderingEngine.RenderBlockInput;
import Kurama.scene.Scene;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.glBlendFunc;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20C.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL31.glDrawArraysInstanced;
import static org.lwjgl.opengl.GL31.glDrawElementsInstanced;
import static org.lwjgl.opengl.GL33.glVertexAttribDivisor;

public class DefaultRenderPipeline extends Kurama.renderingEngine.RenderPipeline {

    public static String sceneShaderBlockID = "sceneShaderBlock";
    public static String hudShaderBlockID = "hudShaderBlock";
    public static String skyboxShaderBlockID = "skyboxShaderBlock";
    public static String particleShaderBlockID = "particleShaderBlock";

    SceneShaderBlock sceneShaderBlock = new SceneShaderBlock(sceneShaderBlockID, this);
    HUD_ShaderBlock hudShaderBlock = new HUD_ShaderBlock(hudShaderBlockID, this);
    SkyboxShaderBlock skyboxShaderBlock = new SkyboxShaderBlock(skyboxShaderBlockID, this);
    ParticleShaderBlock particleShaderBlock = new ParticleShaderBlock(particleShaderBlockID, this);

    public static final int FLOAT_SIZE_BYTES = 4;
    public static final int VECTOR4F_SIZE_BYTES = 4 * FLOAT_SIZE_BYTES;
    public static final int MATRIX_SIZE_BYTES = 4 * VECTOR4F_SIZE_BYTES;
    public static final int MATRIX_SIZE_FLOATS = 16;
    public static final int INSTANCE_SIZE_BYTES = MATRIX_SIZE_BYTES + (1*VECTOR4F_SIZE_BYTES);
    public static final int INSTANCE_SIZE_FLOATS = MATRIX_SIZE_FLOATS + (1*4);

    public DefaultRenderPipeline(Game game) {
        super(game);

    }

    @Override
    public void setup(Scene scene) {
        sceneShaderBlock.setup(new RenderBlockInput(scene, game));
        skyboxShaderBlock.setup(new RenderBlockInput(scene, game));
        hudShaderBlock.setup(new RenderBlockInput(scene, game));
        particleShaderBlock.setup(new RenderBlockInput(scene, game));

        renderBlockID_renderBlock_map.put(sceneShaderBlockID, sceneShaderBlock);
        renderBlockID_renderBlock_map.put(skyboxShaderBlockID, skyboxShaderBlock);
        renderBlockID_renderBlock_map.put(hudShaderBlockID, hudShaderBlock);
        renderBlockID_renderBlock_map.put(particleShaderBlockID, particleShaderBlock);

        glEnable(GL_DEPTH_TEST);    //Enables depth testing

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
//        glBlendFunc(GL_ONE_MINUS_DST_ALPHA,GL_DST_ALPHA);
//        glBlendFunc(GL_SRC_ALPHA, GL_ONE);

        enable(GL_CULL_FACE);
        setCullFace(GL_BACK);
    }

    public void enable(int param) {
        glEnable(param);
    }
    public void disable(int param) {
        glDisable(param);
    }
    public void setCullFace(int param) {
        glCullFace(param);
    }

    @Override
    public void render(Scene scene) {
//        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        //        glBlendFunc(GL_ONE_MINUS_DST_ALPHA,GL_DST_ALPHA);
        sceneShaderBlock.render(new RenderBlockInput(scene, game));
        skyboxShaderBlock.render(new RenderBlockInput(scene, game));

//        glDisable(GL_CULL_FACE);
        particleShaderBlock.render(new RenderBlockInput(scene, game));
//        glEnable(GL_CULL_FACE);

        //Hud should be rendered at last, or else text would have background
//        glBlendFunc(GL_SRC_ALPHA, GL_ONE);
        hudShaderBlock.render(new RenderBlockInput(scene, game));

    }

    @Override
    public void cleanUp() {
        sceneShaderBlock.cleanUp();
        skyboxShaderBlock.cleanUp();
        hudShaderBlock.cleanUp();
        particleShaderBlock.cleanUp();
    }

    @Override
    public void initializeMesh(Mesh mesh) {
        if(mesh instanceof InstancedMesh) {
            initializeInstancedMesh((InstancedMesh) mesh);
        }
        else {
            initializeRegularMesh(mesh);
        }
    }

    public void initToEndFullRender(Mesh mesh, int offset) {
        for(Material material:mesh.materials) {
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
        glBindVertexArray(mesh.vaoId);
        render(mesh);
        endRender(mesh);
    }

    public void render(Mesh mesh) {
        if(mesh.indices != null) {
            glDrawElements(mesh.drawMode, mesh.indices.size(), GL_UNSIGNED_INT, 0);
        }
        else {
            glDrawArrays(mesh.drawMode, 0, mesh.getVertices().size());
        }
    }

    public void renderInstanced(InstancedMesh mesh, int numModels) {
        if(mesh.indices != null) {
            glDrawElementsInstanced(mesh.drawMode, mesh.indices.size(), GL_UNSIGNED_INT, 0, numModels);
        }
        else {
            glDrawArraysInstanced(mesh.drawMode, 0, mesh.getVertices().size(), numModels);
        }
    }

    public int initRender(Mesh mesh, int offset) {

        for(Material material:mesh.materials) {
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
        glBindVertexArray(mesh.vaoId);
        return offset;
    }

    public void initRender(Mesh mesh) {
        glBindVertexArray(mesh.vaoId);
    }

    public void endRender(Mesh mesh) {
        glBindVertexArray(0);
    }

    public void initializeRegularMesh(Mesh mesh) {

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

        if(!mesh.isAttributePresent(Mesh.WEIGHTBIASESPERVERT)) {
            Vector negs = new Vector(MD5Utils.MAXWEIGHTSPERVERTEX, -1);
            List<Vector> att = new ArrayList<>(mesh.vertAttributes.get(Mesh.POSITION).size());
            att.add(negs);
            for(int i = 0;i < mesh.indices.size(); i++) {
                att.add(negs);
            }
            mesh.setAttribute(att, Mesh.WEIGHTBIASESPERVERT);
            mesh.setAttribute(att, Mesh.JOINTINDICESPERVERT);

            for(var f: mesh.faces) {
                for(var vert: f.vertices) {
                    vert.setAttribute(0, Vertex.WEIGHTBIASESPERVERT);
                    vert.setAttribute(0, Vertex.JOINTINDICESPERVERT);
                }
            }
        }

        IntBuffer indicesBuffer = null;
        List<Integer> offsets = new ArrayList<>(mesh.vertAttributes.size());
        List<Integer> sizePerAttrib = new ArrayList<>(mesh.vertAttributes.size());
        int stride = 0;

        final int sizeOfFloat = Float.SIZE / Byte.SIZE;
        try {
//        Calculate stride and offset
            offsets.add(0);
            for(int i = 0;i < mesh.vertAttributes.size();i++) {
                Vector curr = null;
                int numberOfElements = 0;

                if(curr == null) {
//                break;
                    if(mesh.vertAttributes.get(i)!= null) {
                        for (int j = 0; j < mesh.vertAttributes.get(i).size(); j++) {
                            curr = mesh.vertAttributes.get(i).get(j);
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

            mesh.vaoId = glGenVertexArrays();
            glBindVertexArray(mesh.vaoId);

            for(int i = 0;i < sizePerAttrib.size();i++) {
                if(mesh.vertAttributes.get(i)!=null) {

                    FloatBuffer tempBuffer = MemoryUtil.memAllocFloat(sizePerAttrib.get(i) * mesh.vertAttributes.get(i).size());
                    for (Vector v : mesh.vertAttributes.get(i)) {
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
                    mesh.vboIdList.add(vboId);
                    glBindBuffer(GL_ARRAY_BUFFER, vboId);
                    glBufferData(GL_ARRAY_BUFFER, tempBuffer, GL_STATIC_DRAW);
                    glEnableVertexAttribArray(i);
                    glVertexAttribPointer(i, sizePerAttrib.get(i) / sizeOfFloat, GL_FLOAT, false, 0, 0);

                    MemoryUtil.memFree(tempBuffer);   //Free buffer

                }
            }

//            INDEX BUFFER
            if(mesh.indices != null) {
//                int vboId;
                indicesBuffer = MemoryUtil.memAllocInt(mesh.indices.size());
                for(int i:mesh.indices) {
                    indicesBuffer.put(i);
                }
                indicesBuffer.flip();

                vboId = glGenBuffers();
                mesh.vboIdList.add(vboId);
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
        }

    }

    public void initializeInstancedMesh(InstancedMesh mesh) {
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

        IntBuffer indicesBuffer;
        List<Integer> offsets = new ArrayList<>(mesh.vertAttributes.size());
        List<Integer> sizePerAttrib = new ArrayList<>(mesh.vertAttributes.size());
        int stride = 0;

        if(!mesh.isAttributePresent(Mesh.WEIGHTBIASESPERVERT)) {
            Vector negs = new Vector(MD5Utils.MAXWEIGHTSPERVERTEX, 0);
            List<Vector> att = new ArrayList<>(mesh.vertAttributes.get(Mesh.POSITION).size());
            for(var ind: mesh.indices) {
                att.add(null);
            }

            mesh.setAttribute(att, Mesh.WEIGHTBIASESPERVERT);
            mesh.setAttribute(att, Mesh.JOINTINDICESPERVERT);

            for(var f: mesh.faces) {
                for(var vert: f.vertices) {
                    vert.setAttribute(0, Vertex.WEIGHTBIASESPERVERT);
                    vert.setAttribute(0, Vertex.JOINTINDICESPERVERT);
                }
            }
        }

        final int sizeOfFloat = Float.SIZE / Byte.SIZE;

//        Calculate stride and offset
        offsets.add(0);
        try {
            for(int i = 0;i < mesh.vertAttributes.size();i++) {
                Vector curr = null;
                int numberOfElements = 0;

                if(curr == null) {
//                break;
                    if(mesh.vertAttributes.get(i)!= null) {
                        for (int j = 0; j < mesh.vertAttributes.get(i).size(); j++) {
                            curr = mesh.vertAttributes.get(i).get(j);
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

            int attribIndex = 0;  //Keeps track of vertex attribute index
            mesh.vaoId = glGenVertexArrays();
            glBindVertexArray(mesh.vaoId);

            for(int i = 0;i < sizePerAttrib.size();i++) {
                if(mesh.vertAttributes.get(i)!=null) {

                    FloatBuffer tempBuffer = MemoryUtil.memAllocFloat(sizePerAttrib.get(i) * mesh.vertAttributes.get(i).size());
                    for (Vector v : mesh.vertAttributes.get(i)) {
                        if (v != null) {
                            tempBuffer.put(v.getData());
                        } else {    //Hack to handle nulls
                            float[] t = defaultVals.get(i).getData();
                            tempBuffer.put(defaultVals.get(i).getData());
                        }
                    }

                    tempBuffer.flip();

                    vboId = glGenBuffers();
                    mesh.vboIdList.add(vboId);
                    glBindBuffer(GL_ARRAY_BUFFER, vboId);
                    glBufferData(GL_ARRAY_BUFFER, tempBuffer, GL_STATIC_DRAW);
                    glEnableVertexAttribArray(attribIndex);
                    GL20.glVertexAttribPointer(attribIndex, sizePerAttrib.get(i) / sizeOfFloat, GL_FLOAT, false, 0, 0);

                    MemoryUtil.memFree(tempBuffer);   //Free buffer

                    attribIndex++;
                }
            }

            if(mesh.indices != null) {
                indicesBuffer = MemoryUtil.memAllocInt(mesh.indices.size());
                for(int i:mesh.indices) {
                    indicesBuffer.put(i);
                }
                indicesBuffer.flip();

                vboId = glGenBuffers();
                mesh.vboIdList.add(vboId);
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboId);
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);
                MemoryUtil.memFree(indicesBuffer);  //Free buffer
            }

//            Set up per instance vertex attributes such as transformation matrices

            // Model To World matrices
            int strideStart = 0;
            mesh.instanceDataVBO = glGenBuffers();
            mesh.vboIdList.add(mesh.instanceDataVBO);
            mesh.instanceDataBuffer = MemoryUtil.memAllocFloat(mesh.instanceChunkSize * INSTANCE_SIZE_FLOATS);
            glBindBuffer(GL_ARRAY_BUFFER, mesh.instanceDataVBO);
            for(int i = 0;i < 4; i++) {
                GL20.glVertexAttribPointer(attribIndex, 4, GL_FLOAT, false, INSTANCE_SIZE_BYTES, strideStart);
                glVertexAttribDivisor(attribIndex, 1);
                glEnableVertexAttribArray(attribIndex);
                attribIndex++;
                strideStart += VECTOR4F_SIZE_BYTES;
            }

            // Material global ind and atlas offset
            for(int i = 0;i < 1; i++) {
                GL20.glVertexAttribPointer(attribIndex, 4, GL_FLOAT, false, INSTANCE_SIZE_BYTES, strideStart);
                glVertexAttribDivisor(attribIndex, 1);
                glEnableVertexAttribArray(attribIndex);
                attribIndex++;
                strideStart += VECTOR4F_SIZE_BYTES;
            }

            glBindBuffer(GL_ARRAY_BUFFER,0);
            glBindVertexArray(0);

        }
        catch(Exception e) {
            System.out.println("caught exception here");
            System.exit(1);
        }finally{

        }

    }
}
