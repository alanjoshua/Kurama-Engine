package Kurama.renderingEngine.defaultRenderPipeline;

import Kurama.Math.FrustumIntersection;
import Kurama.Math.Vector;
import Kurama.Mesh.Material;
import Kurama.Mesh.Mesh;
import Kurama.game.Game;
import Kurama.geometry.MD5.MD5Utils;
import Kurama.ComponentSystem.components.model.Model;
import Kurama.particle.ParticleGenerator;
import Kurama.renderingEngine.*;
import Kurama.scene.Scene;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.glBlendFunc;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20C.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.glDrawArraysInstanced;
import static org.lwjgl.opengl.GL31.glDrawElementsInstanced;
import static org.lwjgl.opengl.GL33.glVertexAttribDivisor;
import static org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BUFFER;
import static org.lwjgl.opengl.GL44C.GL_DYNAMIC_STORAGE_BIT;
import static org.lwjgl.opengl.GL45C.glCreateBuffers;
import static org.lwjgl.opengl.GL45C.glNamedBufferStorage;

public class DefaultRenderPipeline extends Kurama.renderingEngine.RenderPipeline {

    public static String sceneShaderBlockID = "sceneShaderBlock";
    public static String shadowBlockID = "shadowBlock";
    public static String skyboxShaderBlockID = "skyboxShaderBlock";
    public static String particleShaderBlockID = "particleShaderBlock";

    SceneShaderBlock sceneShaderBlock;
    ShadowBlock shadowBlock;
    SkyboxShaderBlock skyboxShaderBlock;
    ParticleShaderBlock particleShaderBlock;

    public static int MAX_DIRECTIONAL_LIGHTS = 5;
    public static int MAX_SPOTLIGHTS = 10;
    public static int MAX_POINTLIGHTS = 10;
    public static int MAX_JOINTS = 150;
    public static int MAX_MATERIALS = 50;

    public static final int FLOAT_SIZE_BYTES = 4;
    public static final int VECTOR4F_SIZE_BYTES = 4 * FLOAT_SIZE_BYTES;
    public static final int MATRIX_SIZE_BYTES = 4 * VECTOR4F_SIZE_BYTES;
    public static final int MATRIX_SIZE_FLOATS = 16;

    public static int MAX_INSTANCED_SKELETAL_MESHES = 50;

    public static final int INSTANCE_SIZE_BYTES = MATRIX_SIZE_BYTES + (1*VECTOR4F_SIZE_BYTES);
    public static final int INSTANCE_SIZE_FLOATS = MATRIX_SIZE_FLOATS + (1*4);

    public FrustumIntersection frustumIntersection = new FrustumIntersection();
    public int jointsInstancedBufferID;

    public DefaultRenderPipeline(Game game, RenderPipeline parentPipeline, String pipelineID) {
        super(game, parentPipeline, pipelineID);
    }

    @Override
    public void setup(RenderPipelineData input) {

        var scene = input.scene;
        sceneShaderBlock = new SceneShaderBlock(game, this, sceneShaderBlockID);
        shadowBlock = new ShadowBlock(game, this, shadowBlockID);
        skyboxShaderBlock = new SkyboxShaderBlock(game, this, skyboxShaderBlockID);
        particleShaderBlock = new ParticleShaderBlock(game, this, particleShaderBlockID);

        setupSkeletonSSBO();

        sceneShaderBlock.setup(new RenderPipelineData(scene, game));
        shadowBlock.setup(new RenderPipelineData(scene, game));
        skyboxShaderBlock.setup(new RenderPipelineData(scene, game));
        particleShaderBlock.setup(new RenderPipelineData(scene, game));

        renderBlocks.add(sceneShaderBlock);
        renderBlocks.add(shadowBlock);
        renderBlocks.add(skyboxShaderBlock);
        renderBlocks.add(particleShaderBlock);

        glEnable(GL_DEPTH_TEST);    //Enables depth testing

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        enable(GL_CULL_FACE);
        setCullFace(GL_BACK);
    }

    public void setupSkeletonSSBO() {

        jointsInstancedBufferID = glCreateBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, jointsInstancedBufferID);

        var jointsDataInstancedBuffer = MemoryUtil.memAllocFloat(
                DefaultRenderPipeline.MAX_INSTANCED_SKELETAL_MESHES *
                        DefaultRenderPipeline.MAX_JOINTS *
                        DefaultRenderPipeline.MATRIX_SIZE_FLOATS);

        glNamedBufferStorage(jointsInstancedBufferID, jointsDataInstancedBuffer, GL_DYNAMIC_STORAGE_BIT);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, jointsInstancedBufferID);

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        MemoryUtil.memFree(jointsDataInstancedBuffer);
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
    public RenderPipelineData render(RenderPipelineData input) {
        var scene = input.scene;

        glCullFace(GL_FRONT);
        var shadowOut = shadowBlock.render(new RenderPipelineData(scene, game));
        glCullFace(GL_BACK);

        for(var camera: input.scene.cameras) {

            if(camera.isActive) {

                if(camera.shouldPerformFrustumCulling) {
                    frustumIntersection.set(camera.getPerspectiveProjectionMatrix().matMul(camera.getWorldToObject()));
                    frustumCullModels(input.scene.shaderblock_mesh_model_map.get(sceneShaderBlockID), input.scene);
                    frustumCullParticles(input.scene.particleGenerators);
                }

                var shaderInput = new CurrentCameraBlockData(scene, game, camera, ((ShadowPackageData)shadowOut).shadowPackage);

                glBindFramebuffer(GL_FRAMEBUFFER, camera.renderBuffer.fboId);
                glViewport(0, 0, camera.renderResolution.geti(0), camera.renderResolution.geti(1));
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

                glEnable(GL_CULL_FACE);
                glCullFace(GL_BACK);
                sceneShaderBlock.render(shaderInput);
                glEnable(GL_CULL_FACE);
                glCullFace(GL_BACK);

                skyboxShaderBlock.render(shaderInput);
                particleShaderBlock.render(shaderInput);
            }
        }

        return null;
    }

    public void frustumCullModels(Map<String, HashMap<String, Model>> mesh_model_map, Scene scene) {

        for ( var meshID: mesh_model_map.keySet()) {
            var mesh = scene.meshID_mesh_map.get(meshID);
            var meshBoundingRadius = mesh.boundingRadius;

            for (var modelID : mesh_model_map.get(meshID).keySet()) {
                var model = scene.modelID_model_map.get(modelID);

                if(model.shouldRender && model.shouldBeConsideredForFrustumCulling) {
                    var radius = model.scale.getNorm() * meshBoundingRadius;
                    model.isInsideFrustum = frustumIntersection.testSphere(model.pos, radius);
                }

            }
        }
    }

    public void frustumCullParticles(List<ParticleGenerator> generators) {
        for(var gen: generators) {
            for(var part: gen.particles) {
                part.isInsideFrustum = frustumIntersection.testPoint(part.pos);
            }
        }
    }

    @Override
    public void cleanUp() {
        shadowBlock.cleanUp();
        sceneShaderBlock.cleanUp();
        skyboxShaderBlock.cleanUp();
//        hudShaderBlock.cleanUp();
        particleShaderBlock.cleanUp();
//        fullScreenQuadBlock.cleanUp();
    }

    @Override
    public void initializeMesh(Mesh mesh) {
        if(mesh.isInstanced) {
            initializeInstancedMesh(mesh);
        }
        else {
            initializeRegularMesh(mesh);
        }
    }

    public static void initToEndFullRender(Mesh mesh, int offset) {
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

    public static void render(Mesh mesh) {
        if(mesh.indices != null) {
            glDrawElements(mesh.drawMode, mesh.indices.size(), GL_UNSIGNED_INT, 0);
        }
        else {
            glDrawArrays(mesh.drawMode, 0, mesh.getVertices().size());
        }
    }

    public static void renderInstanced(Mesh mesh, int numModels) {
        if(mesh.indices != null) {
            glDrawElementsInstanced(mesh.drawMode, mesh.indices.size(), GL_UNSIGNED_INT, 0, numModels);
        }
        else {
            glDrawArraysInstanced(mesh.drawMode, 0, mesh.getVertices().size(), numModels);
        }
    }

    public static int initRender(Mesh mesh, int offset) {

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

    public static void initRender(Mesh mesh) {
        glBindVertexArray(mesh.vaoId);
    }

    public static void endRender(Mesh mesh) {
        glBindVertexArray(0);
    }

    public static void initializeRegularMesh(Mesh mesh) {

        List<Vector> defaultVals = new ArrayList<>();
        defaultVals.add(new Vector(0,0,0));
        defaultVals.add(new Vector(2,0));
        defaultVals.add(new Vector(0,0,0));
        defaultVals.add(new Vector(0,0,0, 1));
        defaultVals.add(new Vector(0,0,0));
        defaultVals.add(new Vector(0,0,0));
        defaultVals.add(new Vector(new float[]{0}));
        defaultVals.add(new Vector(MD5Utils.MAXWEIGHTSPERVERTEX, -1));
        defaultVals.add(new Vector(MD5Utils.MAXWEIGHTSPERVERTEX, -1));

        IntBuffer indicesBuffer = null;
        List<Integer> offsets = new ArrayList<>(mesh.vertAttributes.size());
        List<Integer> sizePerAttrib = new ArrayList<>(mesh.vertAttributes.size());
        int stride = 0;

        final int sizeOfFloat = Float.SIZE / Byte.SIZE;
        try {
//        Calculate stride and offset
            offsets.add(0);

            for(int i = 0;i < defaultVals.size();i++) {
                Vector curr = mesh.isAttributePresent(i) ?
                        (mesh.vertAttributes.get(i).get(0) == null?
                                defaultVals.get(i): mesh.vertAttributes.get(i).get(0)): defaultVals.get(i);
                int numberOfElements = curr.getNumberOfDimensions();
                int size = numberOfElements * sizeOfFloat;
                stride += size;
                sizePerAttrib.add(size/sizeOfFloat);
                offsets.add(stride);
            }
            offsets.remove(offsets.size() - 1);

            int vboId;
            mesh.vaoId = glGenVertexArrays();
            glBindVertexArray(mesh.vaoId);

            for(int i = 0;i < sizePerAttrib.size();i++) {

                FloatBuffer tempBuffer = null;

                if(mesh.isAttributePresent(i) && mesh.vertAttributes.get(i)!=null) {
                    tempBuffer = MemoryUtil.memAllocFloat(sizePerAttrib.get(i) * mesh.vertAttributes.get(i).size());
                    for (Vector v : mesh.vertAttributes.get(i)) {
                        if (v != null) {
                            tempBuffer.put(v.getData());
                        } else {    //Hack to handle nulls
                            tempBuffer.put(defaultVals.get(i).getData());
                        }
                    }

                }
                else {
                    tempBuffer = MemoryUtil.memAllocFloat(sizePerAttrib.get(i) * mesh.vertAttributes.get(Mesh.POSITION).size());
                    for(var temp: mesh.vertAttributes.get(Mesh.POSITION)) {
                        defaultVals.get(i).setValuesToBuffer(tempBuffer);
                    }
                }

                tempBuffer.flip();
                vboId = glGenBuffers();
                mesh.vboIdList.add(vboId);
                glBindBuffer(GL_ARRAY_BUFFER, vboId);
                glBufferData(GL_ARRAY_BUFFER, tempBuffer, GL_STATIC_DRAW);
                glEnableVertexAttribArray(i);
                glVertexAttribPointer(i, sizePerAttrib.get(i), GL_FLOAT, false, 0, 0);

                MemoryUtil.memFree(tempBuffer);   //Free buffer
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

    public static void initializeInstancedMesh(Mesh mesh) {
        List<Vector> defaultVals = new ArrayList<>();
        defaultVals.add(new Vector(0,0,0));
        defaultVals.add(new Vector(2,0));
        defaultVals.add(new Vector(0,0,0));
        defaultVals.add(new Vector(0,0,0, 1));
        defaultVals.add(new Vector(0,0,0));
        defaultVals.add(new Vector(0,0,0));
        defaultVals.add(new Vector(new float[]{0}));
        defaultVals.add(new Vector(MD5Utils.MAXWEIGHTSPERVERTEX, -1));
        defaultVals.add(new Vector(MD5Utils.MAXWEIGHTSPERVERTEX, -1));

        IntBuffer indicesBuffer = null;
        List<Integer> offsets = new ArrayList<>(mesh.vertAttributes.size());
        List<Integer> sizePerAttrib = new ArrayList<>(mesh.vertAttributes.size());
        int stride = 0;

        final int sizeOfFloat = Float.SIZE / Byte.SIZE;
        try {
//        Calculate stride and offset
            offsets.add(0);
            for(int i = 0;i < defaultVals.size();i++) {
                Vector curr = mesh.isAttributePresent(i) ?
                        (mesh.vertAttributes.get(i).get(0) == null?
                                defaultVals.get(i): mesh.vertAttributes.get(i).get(0)): defaultVals.get(i);
                int numberOfElements = curr.getNumberOfDimensions();
                int size = numberOfElements * sizeOfFloat;
                stride += size;
                sizePerAttrib.add(size/sizeOfFloat);
                offsets.add(stride);
            }
            offsets.remove(offsets.size() - 1);

            int vboId;

            int attribIndex = 0;
            mesh.vaoId = glGenVertexArrays();
            glBindVertexArray(mesh.vaoId);

            for(int i = 0;i < sizePerAttrib.size();i++) {

                FloatBuffer tempBuffer = null;

                if(mesh.isAttributePresent(i) && mesh.vertAttributes.get(i)!=null) {
                    tempBuffer = MemoryUtil.memAllocFloat(sizePerAttrib.get(i) * mesh.vertAttributes.get(i).size());
                    for (Vector v : mesh.vertAttributes.get(i)) {
                        if (v != null) {
                            tempBuffer.put(v.getData());
                        } else {    //Hack to handle nulls
                            tempBuffer.put(defaultVals.get(i).getData());
                        }
                    }

                }
                else {
                    tempBuffer = MemoryUtil.memAllocFloat(sizePerAttrib.get(i) * mesh.vertAttributes.get(Mesh.POSITION).size());
                    for(var temp: mesh.vertAttributes.get(Mesh.POSITION)) {
                        defaultVals.get(i).setValuesToBuffer(tempBuffer);
                    }
                }

                tempBuffer.flip();
                vboId = glGenBuffers();
                mesh.vboIdList.add(vboId);
                glBindBuffer(GL_ARRAY_BUFFER, vboId);
                glBufferData(GL_ARRAY_BUFFER, tempBuffer, GL_STATIC_DRAW);
                glEnableVertexAttribArray(i);
                glVertexAttribPointer(i, sizePerAttrib.get(i), GL_FLOAT, false, 0, 0);

                MemoryUtil.memFree(tempBuffer);   //Free buffer
                attribIndex++;

            }

//            INDEX BUFFER
            if(mesh.indices != null) {
//                int vboId;
                indicesBuffer = MemoryUtil.memAllocInt(mesh.indices.size());
                for (int i : mesh.indices) {
                    indicesBuffer.put(i);
                }
                indicesBuffer.flip();

                vboId = glGenBuffers();
                mesh.vboIdList.add(vboId);
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboId);
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);

//                glBindBuffer(GL_ARRAY_BUFFER, 0);
//                glBindVertexArray(0);
                MemoryUtil.memFree(indicesBuffer);
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
            e.printStackTrace();
            System.exit(1);
        }finally{

        }

    }
}
