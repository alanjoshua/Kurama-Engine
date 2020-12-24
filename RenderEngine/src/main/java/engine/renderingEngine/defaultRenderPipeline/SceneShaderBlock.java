package engine.renderingEngine.defaultRenderPipeline;

import engine.Effects.Material;
import engine.Math.Matrix;
import engine.Math.Vector;
import engine.Mesh.InstancedMesh;
import engine.Mesh.Mesh;
import engine.lighting.DirectionalLight;
import engine.lighting.PointLight;
import engine.lighting.SpotLight;
import engine.model.AnimatedModel;
import engine.model.Model;
import engine.renderingEngine.*;
import engine.scene.Scene;
import engine.shader.ShaderProgram;
import engine.utils.Logger;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BUFFER;
import static org.lwjgl.opengl.GL44C.GL_DYNAMIC_STORAGE_BIT;
import static org.lwjgl.opengl.GL45.glNamedBufferStorage;
import static org.lwjgl.opengl.GL45.glNamedBufferSubData;
import static org.lwjgl.opengl.GL45C.glCreateBuffers;

//import static org.lwjgl.opengl.GL30C.*;

public class SceneShaderBlock extends engine.renderingEngine.RenderBlock {

    String shadow_ShaderID = "shadow_shader";
    String scene_shader_id = "scene_shader";

    public ShaderProgram shadow_shader;
    public ShaderProgram scene_shader;

    public static int MAX_DIRECTIONAL_LIGHTS = 5;
    public static int MAX_SPOTLIGHTS = 10;
    public static int MAX_POINTLIGHTS = 10;
    public static int MAX_JOINTS = 150;

    public static final int FLOAT_SIZE_BYTES = 4;
    public static final int VECTOR4F_SIZE_BYTES = 4 * FLOAT_SIZE_BYTES;
    public static final int MATRIX_SIZE_BYTES = 4 * VECTOR4F_SIZE_BYTES;
    public static final int MATRIX_SIZE_FLOATS = 16;

    public static int MAX_INSTANCED_SKELETAL_MESHES = 50;
    public int jointsInstancedBufferID;


    public SceneShaderBlock(String id, RenderPipeline pipeline) {
        super(id, pipeline);
    }

    @Override
    public void setup(RenderBlockInput input) {
        setupSceneShader();
        setupShadowShader();
        setupSKeletonSSBO();
    }

    //    Sets up SSBO to store instanced joint transformation matrices.
    public void setupSKeletonSSBO() {

        jointsInstancedBufferID = glCreateBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, jointsInstancedBufferID);

        var jointsDataInstancedBuffer = MemoryUtil.memAllocFloat(MAX_INSTANCED_SKELETAL_MESHES * SceneShaderBlock.MAX_JOINTS * MATRIX_SIZE_BYTES);
        glNamedBufferStorage(jointsInstancedBufferID, jointsDataInstancedBuffer, GL_DYNAMIC_STORAGE_BIT);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, jointsInstancedBufferID);

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        MemoryUtil.memFree(jointsDataInstancedBuffer);
    }

    @Override
    public void render(RenderBlockInput input) {
        glCullFace(GL_FRONT);  //this means meshes with no back face will not cast shadows.
        ShadowDepthRenderPackage shadowPackage =  renderDepthMap(input.scene);
        glCullFace(GL_BACK);

        glViewport(0,0,input.game.getDisplay().getWidth(),input.game.getDisplay().getHeight());
        RenderingEngineGL.clear();
        renderScene(input.scene,shadowPackage);
    }

    @Override
    public void cleanUp() {
        shadow_shader.cleanUp();
        scene_shader.cleanUp();
    }

    public void setupSceneShader() {

        scene_shader = new ShaderProgram(scene_shader_id);

        try {

            scene_shader.createVertexShader("src/main/java/engine/renderingEngine/defaultShaders/SceneVertexShader.glsl");
            scene_shader.createFragmentShader("src/main/java/engine/renderingEngine/defaultShaders/SceneFragmentShader.glsl");
            scene_shader.link();

            scene_shader.createUniform("projectionMatrix");

//            scene_shader.createUniformArray("modelLightViewMatrix",MAX_DIRECTIONAL_LIGHTS);
//            scene_shader.createUniformArray("modelSpotLightViewMatrix",MAX_SPOTLIGHTS);
            scene_shader.createUniformArray("directionalShadowMaps",MAX_DIRECTIONAL_LIGHTS);
            scene_shader.createUniformArray("spotLightShadowMaps",MAX_SPOTLIGHTS);
            scene_shader.createUniform("numDirectionalLights");
            scene_shader.createUniform("numberOfSpotLights");
            scene_shader.createUniform("isAnimated");
            scene_shader.createMaterialListUniform("materials","mat_textures","mat_normalMaps","mat_diffuseMaps","mat_specularMaps",26);
            scene_shader.createUniform("isInstanced");
            scene_shader.createUniform("ambientLight");

            scene_shader.createPointLightListUniform("pointLights",MAX_POINTLIGHTS);
            scene_shader.createDirectionalLightListUniform("directionalLights",MAX_DIRECTIONAL_LIGHTS);
            scene_shader.createSpotLightListUniform("spotLights",MAX_SPOTLIGHTS);

            scene_shader.createFogUniform("fog");
            scene_shader.createUniform("allDirectionalLightStatic");

            scene_shader.createUniformArray("worldToDirectionalLightMatrix", MAX_DIRECTIONAL_LIGHTS);
            scene_shader.createUniformArray("worldToSpotlightMatrix", MAX_SPOTLIGHTS);
            scene_shader.createUniformArray("jointMatrices", MAX_JOINTS);
            scene_shader.createUniform("modelToWorldMatrix");
            scene_shader.createUniform("worldToCam");

            scene_shader.createUniform("materialsGlobalLoc");
//            scene_shader.createUniform("materialsAtlas");

        }catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void setupShadowShader() {
        shadow_shader = new ShaderProgram(shadow_ShaderID);
        try {
            shadow_shader.createVertexShader("src/main/java/engine/renderingEngine/defaultShaders/depthDirectionalLightVertexShader.glsl");
            shadow_shader.createFragmentShader("src/main/java/engine/renderingEngine/defaultShaders/depthDirectionalLightFragmentShader.glsl");
            shadow_shader.link();

            shadow_shader.createUniform("projectionMatrix");
            shadow_shader.createUniform("modelLightViewMatrix");
            shadow_shader.createUniformArray("jointMatrices", MAX_JOINTS);
            shadow_shader.createUniform("isAnimated");
            shadow_shader.createUniform("isInstanced");

        }catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public int activateMaterialTextures(List<Material> materials, int offset) {
        for(int i = 0;i < materials.size();i++) {

            var material = materials.get(i);
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
            offset += 4;
        }
        return offset;
    }

    public void renderScene(Scene scene, ShadowDepthRenderPackage shadowPackage) {

        DefaultRenderPipeline pipeline = (DefaultRenderPipeline) renderPipeline;
        boolean curShouldCull = true;
        int currCull = GL_BACK;

        glEnable(GL_CULL_FACE);
        glCullFace(currCull);

        ShaderProgram sceneShaderProgram = scene_shader;
        sceneShaderProgram.bind();

        Matrix worldToCam = scene.camera.getWorldToCam();
        Matrix projectionMatrix = scene.camera.getPerspectiveProjectionMatrix();

        sceneShaderProgram.setUniform("projectionMatrix",projectionMatrix);
        sceneShaderProgram.setUniform("worldToCam", worldToCam);

        sceneShaderProgram.setUniform("ambientLight",scene.ambientLight);

        LightDataPackage lights = processLights(scene.pointLights, scene.spotLights, scene.directionalLights, worldToCam);
        sceneShaderProgram.setUniform("spotLights",lights.spotLights);
        sceneShaderProgram.setUniform("pointLights",lights.pointLights);
        sceneShaderProgram.setUniform("directionalLights",lights.directionalLights);
        sceneShaderProgram.setUniform("numDirectionalLights",scene.directionalLights.size());
        sceneShaderProgram.setUniform("numberOfSpotLights",scene.spotLights.size());
        sceneShaderProgram.setUniform("fog", scene.fog);

        if(scene.hasMatLibraryUpdated) {
            Logger.log("setting materials");
            sceneShaderProgram.setOnlyMaterials_noTextureBinding("materials", "mat_textures",
                    "mat_normalMaps", "mat_diffuseMaps", "mat_specularMaps", scene.materialLibrary, 0);
        }
        int offset = activateMaterialTextures(scene.materialLibrary, 0);

        Vector c = new Vector(3,0);
        for(int i = 0;i < scene.directionalLights.size(); i++) {
            DirectionalLight l = scene.directionalLights.get(i);
            c = c.add(l.color.scalarMul(l.intensity));
            sceneShaderProgram.setUniform("worldToDirectionalLightMatrix["+i+"]",
                    l.shadowProjectionMatrix.matMul(shadowPackage.worldToDirectionalLights.get(i)));
        }
        sceneShaderProgram.setUniform("allDirectionalLightStatic", c);

        for(int i = 0;i < scene.spotLights.size(); i++) {
            SpotLight l = scene.spotLights.get(i);
            sceneShaderProgram.setUniform("worldToSpotlightMatrix["+i+"]",
                    l.shadowProjectionMatrix.matMul(shadowPackage.worldToSpotLights.get(i)));
        }

        offset = sceneShaderProgram.setAndActivateDirectionalShadowMaps("directionalShadowMaps", scene.directionalLights,offset);
        offset = sceneShaderProgram.setAndActivateSpotLightShadowMaps("spotLightShadowMaps", scene.spotLights, offset);

        for(String meshId :scene.shaderblock_mesh_model_map.get(blockID).keySet()) {

            Mesh mesh = scene.meshID_mesh_map.get(meshId);
            if (curShouldCull != mesh.shouldCull) {
                if(mesh.shouldCull) {
                    glEnable(GL_CULL_FACE);
                }
                else {
                    glDisable(GL_CULL_FACE);
                }
                curShouldCull = mesh.shouldCull;
            }

            if(currCull != mesh.cullmode) {
                glCullFace(mesh.cullmode);
                currCull = mesh.cullmode;
            }

            pipeline.initRender(mesh);

            if(mesh instanceof InstancedMesh) {

                sceneShaderProgram.setUniform("isInstanced", 1);

                List<Model> models = new ArrayList<>();
                for (String modelId : scene.shaderblock_mesh_model_map.get(blockID).get(meshId).keySet()) {
                    Model model = scene.modelID_model_map.get(modelId);
                    if(model.shouldRender) {
                        models.add(model);
                    }
                }

                var inst_mesh = (InstancedMesh) mesh;
                List<List<Model>> chunks;
                if(mesh.isAnimatedSkeleton) {
                    chunks = InstancedMesh.getRenderChunks(models,
                            inst_mesh.instanceChunkSize < MAX_INSTANCED_SKELETAL_MESHES ?
                                    inst_mesh.instanceChunkSize: MAX_INSTANCED_SKELETAL_MESHES);
                    scene_shader.setUniform("isAnimated", 1);
                }
                else {
                    chunks = InstancedMesh.getRenderChunks(models, inst_mesh.instanceChunkSize);
                    scene_shader.setUniform("isAnimated", 0);
                }
                for (var chunk: chunks) {
                    inst_mesh.instanceDataBuffer.clear();

                    int modelCount = 0;
                    for(Model m: chunk) {

                        if(mesh.isAnimatedSkeleton) {
                            var anim = (AnimatedModel) m;
                            for (int i = 0; i < anim.numJoints; i++) {
                                Matrix jointMat;
                                jointMat = anim.currentJointTransformations.get(i);
//                                }
                                FloatBuffer temp = jointMat.getAsFloatBuffer();
//                                TODO: Change this to persistent map
                                glNamedBufferSubData(jointsInstancedBufferID,
                                        ((modelCount * MAX_JOINTS) + i) * MATRIX_SIZE_BYTES, temp);
                                MemoryUtil.memFree(temp);
                            }
                        }

                        Matrix objectToWorld = m.getObjectToWorldMatrix();
                        objectToWorld.setValuesToBuffer(inst_mesh.instanceDataBuffer);

                        Vector matsGlobalLoc = new Vector(4, 0);
                        for(int i = 0;i < m.materials.get(meshId).size();i++) {
                            matsGlobalLoc.setDataElement(i, m.materials.get(meshId).get(i).globalSceneID);
                        }

//                        Vector matsAtlas = new Vector(4, 0);
//                        for(int i = 0;i < m.matAtlasOffset.get(meshId).size();i++) {
//                            matsAtlas.setDataElement(i, m.matAtlasOffset.get(meshId).get(i));
//                        }

                        for(var v: matsGlobalLoc.getData()) {
                            inst_mesh.instanceDataBuffer.put(v);
                        }
//                        for(var v: matsAtlas.getData()) {
//                            inst_mesh.instanceDataBuffer.put(v);
//                        }
                        modelCount++;
                    }
                    inst_mesh.instanceDataBuffer.flip();
                    glBindBuffer(GL_ARRAY_BUFFER, inst_mesh.instanceDataVBO);
                    glBufferData(GL_ARRAY_BUFFER, inst_mesh.instanceDataBuffer, GL_DYNAMIC_DRAW);

                    pipeline.renderInstanced(inst_mesh, chunk.size());
                }
            }

            else {  // Non instanced meshes

                sceneShaderProgram.setUniform("isInstanced", 0);

                for (String modelId : scene.shaderblock_mesh_model_map.get(blockID).get(meshId).keySet()) {
                    Model model = scene.modelID_model_map.get(modelId);

                    if (model.shouldRender) {

                        if (model instanceof AnimatedModel) {
                            scene_shader.setUniform("isAnimated", 1);

                            AnimatedModel anim = (AnimatedModel) model;
                            for (int i = 0; i < anim.currentJointTransformations.size(); i++) {
                                var matrix = anim.currentJointTransformations.get(i);
                                sceneShaderProgram.setUniform("jointMatrices[" + i + "]", matrix);
                            }
                        } else {
                            scene_shader.setUniform("isAnimated", 0);
                        }

                        Vector matsGlobalLoc = new Vector(4, 0);
                        for(int i = 0;i < model.materials.get(meshId).size();i++) {
                            matsGlobalLoc.setDataElement(i, model.materials.get(meshId).get(i).globalSceneID);
                        }

//                        Vector matsAtlas = new Vector(4, 0);
//                        for(int i = 0;i < model.matAtlasOffset.get(meshId).size();i++) {
//                            matsAtlas.setDataElement(i, model.matAtlasOffset.get(meshId).get(i));
//                        }

                        scene_shader.setUniform("materialsGlobalLoc", matsGlobalLoc);
//                        scene_shader.setUniform("materialsAtlas", matsAtlas);
                        Matrix objectToWorld = model.getObjectToWorldMatrix();
                        scene_shader.setUniform("modelToWorldMatrix", objectToWorld);
//
                        pipeline.render(mesh);
                    }
                }
            }

            pipeline.endRender(mesh);
        }

        sceneShaderProgram.unbind();
    }

    public ShadowDepthRenderPackage renderDepthMap(Scene scene) {

        DefaultRenderPipeline pipeline = (DefaultRenderPipeline) renderPipeline;

        boolean curShouldCull = true;
        int currCull = GL_BACK;

        glEnable(GL_CULL_FACE);
        glCullFace(currCull);

        ShaderProgram depthShaderProgram = shadow_shader;
        depthShaderProgram.bind();
        List<Matrix> worldToDirectionalLights = new ArrayList<>();
        List<Matrix> worldToSpotLights = new ArrayList<>();

        for(int i =0;i < scene.directionalLights.size();i++) {
            DirectionalLight light = scene.directionalLights.get(i);
            Matrix worldToLight = light.getWorldToObject();
            worldToDirectionalLights.add(worldToLight);

            if(light.doesProduceShadow) {

                glViewport(0, 0, light.shadowMap.shadowMapWidth, light.shadowMap.shadowMapHeight);
                glBindFramebuffer(GL_FRAMEBUFFER, light.shadowMap.depthMapFBO);
                glClear(GL_DEPTH_BUFFER_BIT);

                depthShaderProgram.setUniform("projectionMatrix", light.shadowProjectionMatrix);

                for (String meshId : scene.shaderblock_mesh_model_map.get(blockID).keySet()) {

                    Mesh mesh = scene.meshID_mesh_map.get(meshId);

                    if (curShouldCull != mesh.shouldCull) {
                        if (mesh.shouldCull) {
                            glEnable(GL_CULL_FACE);
                        } else {
                            glDisable(GL_CULL_FACE);
                        }
                        curShouldCull = mesh.shouldCull;
                    }

                    if (currCull != mesh.cullmode) {
                        glCullFace(mesh.cullmode);
                        currCull = mesh.cullmode;
                    }

                    pipeline.initRender(mesh);

                    if (mesh instanceof InstancedMesh) {
                        depthShaderProgram.setUniform("isInstanced", 1);
                        var inst_mesh = (InstancedMesh) mesh;
                        var models = new ArrayList<Model>();

                        for (String modelId : scene.shaderblock_mesh_model_map.get(blockID).get(meshId).keySet()) {
                            var m = scene.modelID_model_map.get(modelId);
                            if (m.shouldRender && m.shouldCastShadow) {
                                models.add(m);
                            }
                        }
                        List<List<Model>> chunks;
                        if (mesh.isAnimatedSkeleton) {
                            chunks = InstancedMesh.getRenderChunks(models,
                                    inst_mesh.instanceChunkSize < MAX_INSTANCED_SKELETAL_MESHES ?
                                            inst_mesh.instanceChunkSize : MAX_INSTANCED_SKELETAL_MESHES);
                            depthShaderProgram.setUniform("isAnimated", 1);
                        } else {
                            chunks = InstancedMesh.getRenderChunks(models, inst_mesh.instanceChunkSize);
                            depthShaderProgram.setUniform("isAnimated", 0);
                        }

                        for (var chunk : chunks) {
                            inst_mesh.instanceDataBuffer.clear();

                            int modelCount = 0;
                            for (Model m : chunk) {

                                if (mesh.isAnimatedSkeleton) {
                                    var anim = (AnimatedModel) m;
                                    for (int j = 0; j < anim.numJoints; j++) {
                                        Matrix jointMat;
                                        jointMat = anim.currentJointTransformations.get(j);
//                                }
                                        FloatBuffer temp = jointMat.getAsFloatBuffer();
//                                TODO: Change this to persistent map
                                        glNamedBufferSubData(jointsInstancedBufferID,
                                                ((modelCount * MAX_JOINTS) + j) * MATRIX_SIZE_BYTES, temp);
                                        MemoryUtil.memFree(temp);
                                    }
                                }

                                Matrix objectToLight = worldToLight.matMul(m.getObjectToWorldMatrix());
                                objectToLight.setValuesToBuffer(inst_mesh.instanceDataBuffer);
                                for (int counter = 0; counter < 4; counter++) {
                                    inst_mesh.instanceDataBuffer.put(0f);
                                }
                                modelCount++;
                            }
                            inst_mesh.instanceDataBuffer.flip();

                            glBindBuffer(GL_ARRAY_BUFFER, inst_mesh.instanceDataVBO);
                            glBufferData(GL_ARRAY_BUFFER, inst_mesh.instanceDataBuffer, GL_DYNAMIC_DRAW);

                            pipeline.renderInstanced(inst_mesh, chunk.size());
                        }
                    } else {
                        depthShaderProgram.setUniform("isInstanced", 0);
                        for (String modelId : scene.shaderblock_mesh_model_map.get(blockID).get(meshId).keySet()) {
                            Model m = scene.modelID_model_map.get(modelId);

                            if (m instanceof AnimatedModel) {
                                depthShaderProgram.setUniform("isAnimated", 1);
//                        Logger.log("detecting animated model");
                                AnimatedModel anim = (AnimatedModel) m;
                                for (int j = 0; j < anim.currentJointTransformations.size(); j++) {
                                    var matrix = anim.currentJointTransformations.get(j);
                                    depthShaderProgram.setUniform("jointMatrices[" + j + "]", matrix);
                                }
                            } else {
                                depthShaderProgram.setUniform("isAnimated", 0);
                            }

                            if (m.shouldCastShadow && m.shouldRender) {
                                Matrix modelLightViewMatrix = worldToLight.matMul(m.getObjectToWorldMatrix());
                                depthShaderProgram.setUniform("modelLightViewMatrix", modelLightViewMatrix);
                                pipeline.render(mesh);
                            }
                        }
                    }
                    pipeline.endRender(mesh);
                }
                glBindFramebuffer(GL_FRAMEBUFFER, 0);
            }
        }

        for(int i =0;i < scene.spotLights.size();i++) {

            SpotLight light = scene.spotLights.get(i);
            Matrix worldToLight = light.getWorldToObject();
            worldToSpotLights.add(worldToLight);

            if(light.doesProduceShadow) {

                Matrix projMatrix = light.shadowProjectionMatrix;
                depthShaderProgram.setUniform("projectionMatrix", projMatrix);
                glViewport(0, 0, light.shadowMap.shadowMapWidth, light.shadowMap.shadowMapHeight);
                glBindFramebuffer(GL_FRAMEBUFFER, light.shadowMap.depthMapFBO);
                glClear(GL_DEPTH_BUFFER_BIT);

                for (String meshId : scene.shaderblock_mesh_model_map.get(blockID).keySet()) {
                    Mesh mesh = scene.meshID_mesh_map.get(meshId);

                    if (curShouldCull != mesh.shouldCull) {
                        if (mesh.shouldCull) {
                            glEnable(GL_CULL_FACE);
                        } else {
                            glDisable(GL_CULL_FACE);
                        }
                        curShouldCull = mesh.shouldCull;
                    }

                    if (currCull != mesh.cullmode) {
                        glCullFace(mesh.cullmode);
                        currCull = mesh.cullmode;
                    }

                    pipeline.initRender(mesh);

                    if (mesh instanceof InstancedMesh) {
                        depthShaderProgram.setUniform("isInstanced", 1);
                        var inst_mesh = (InstancedMesh) mesh;
                        var models = new ArrayList<Model>();

                        for (String modelId : scene.shaderblock_mesh_model_map.get(blockID).get(meshId).keySet()) {
                            var m = scene.modelID_model_map.get(modelId);
                            if (m.shouldRender && m.shouldCastShadow) {
                                models.add(m);
                            }
                        }
                        List<List<Model>> chunks;
                        if (mesh.isAnimatedSkeleton) {
                            chunks = InstancedMesh.getRenderChunks(models,
                                    inst_mesh.instanceChunkSize < MAX_INSTANCED_SKELETAL_MESHES ?
                                            inst_mesh.instanceChunkSize : MAX_INSTANCED_SKELETAL_MESHES);
                            depthShaderProgram.setUniform("isAnimated", 1);
                        } else {
                            chunks = InstancedMesh.getRenderChunks(models, inst_mesh.instanceChunkSize);
                            depthShaderProgram.setUniform("isAnimated", 0);
                        }

                        for (var chunk : chunks) {
                            inst_mesh.instanceDataBuffer.clear();

                            int modelCount = 0;
                            for (Model m : chunk) {

                                if (mesh.isAnimatedSkeleton) {
                                    var anim = (AnimatedModel) m;
                                    for (int j = 0; j < anim.numJoints; j++) {
                                        Matrix jointMat;
                                        jointMat = anim.currentJointTransformations.get(j);
//                                }
                                        FloatBuffer temp = jointMat.getAsFloatBuffer();
//                                TODO: Change this to persistent map
                                        glNamedBufferSubData(jointsInstancedBufferID,
                                                ((modelCount * MAX_JOINTS) + j) * MATRIX_SIZE_BYTES, temp);
                                        MemoryUtil.memFree(temp);
                                    }
                                }

                                Matrix objectToLight = worldToLight.matMul(m.getObjectToWorldMatrix());
                                objectToLight.setValuesToBuffer(inst_mesh.instanceDataBuffer);
                                for (int counter = 0; counter < 4; counter++) {
                                    inst_mesh.instanceDataBuffer.put(0f);
                                }
                                modelCount++;
                            }
                            inst_mesh.instanceDataBuffer.flip();

                            glBindBuffer(GL_ARRAY_BUFFER, inst_mesh.instanceDataVBO);
                            glBufferData(GL_ARRAY_BUFFER, inst_mesh.instanceDataBuffer, GL_DYNAMIC_DRAW);

                            pipeline.renderInstanced(inst_mesh, chunk.size());
                        }
                    } else {
                        depthShaderProgram.setUniform("isInstanced", 0);
                        for (String modelId : scene.shaderblock_mesh_model_map.get(blockID).get(meshId).keySet()) {
                            Model m = scene.modelID_model_map.get(modelId);

                            if (m instanceof AnimatedModel) {
                                depthShaderProgram.setUniform("isAnimated", 1);
//                        Logger.log("detecting animated model");
                                AnimatedModel anim = (AnimatedModel) m;
                                for (int j = 0; j < anim.currentJointTransformations.size(); j++) {
                                    var matrix = anim.currentJointTransformations.get(j);
                                    depthShaderProgram.setUniform("jointMatrices[" + j + "]", matrix);
                                }
                            } else {
                                depthShaderProgram.setUniform("isAnimated", 0);
                            }

                            if (m.shouldCastShadow) {
                                Matrix modelLightViewMatrix = worldToLight.matMul(m.getObjectToWorldMatrix());
                                depthShaderProgram.setUniform("modelLightViewMatrix", modelLightViewMatrix);
                                pipeline.render(mesh);
                            }
                        }
                    }
                    pipeline.endRender(mesh);
                }
                glBindFramebuffer(GL_FRAMEBUFFER, 0);
            }
        }

        depthShaderProgram.unbind();
        return new ShadowDepthRenderPackage(worldToDirectionalLights,worldToSpotLights);
    }

    public class ShadowDepthRenderPackage {
        public List<Matrix> worldToDirectionalLights;
        public List<Matrix> worldToSpotLights;
        public ShadowDepthRenderPackage(List<Matrix> worldToDirectionalLights,List<Matrix> worldToSpotLights) {
            this.worldToDirectionalLights = worldToDirectionalLights;
            this.worldToSpotLights = worldToSpotLights;
        }
    }

    public class LightDataPackage {
        public PointLight pointLights[];
        public SpotLight spotLights[];
        public DirectionalLight directionalLights[];
    }

    public LightDataPackage processLights(List<PointLight> pointLights, List<SpotLight> spotLights, List<DirectionalLight> directionalLights, Matrix worldToCam) {
        List<PointLight> pointLightsRes;
        List<SpotLight> spotLightsRes;
        List<DirectionalLight> directionalLightsRes;

        pointLightsRes = pointLights.stream()
                .map(l -> {
                    PointLight currLight = new PointLight(l);
                    currLight.pos = worldToCam.matMul(currLight.pos.append(1)).getColumn(0).removeDimensionFromVec(3);
                    return currLight;
                })
                .collect(Collectors.toList());

        directionalLightsRes = directionalLights.stream()
                .map(l -> {
                    DirectionalLight currDirectionalLight = new DirectionalLight(l);

                    currDirectionalLight.direction_Vector = worldToCam.matMul(currDirectionalLight.getOrientation().
                            getRotationMatrix().getColumn(2).scalarMul(-1).append(0)).
                            getColumn(0).removeDimensionFromVec(3);

                    return currDirectionalLight;
                })
                .collect(Collectors.toList());

        spotLightsRes = spotLights.stream()
                .map(l -> {
                    SpotLight currSpotLight = new SpotLight(l);

                    //Vector dir = new Vector(currSpotLight.coneDirection).addDimensionToVec(0);
                    currSpotLight.coneDirection = worldToCam.matMul(currSpotLight.getOrientation().getRotationMatrix().
                            getColumn(2).scalarMul(-1).append(0)).getColumn(0).removeDimensionFromVec(3);

                    Vector spotLightPos = currSpotLight.pointLight.pos;
                    Vector auxSpot = new Vector(spotLightPos).append(1);
                    currSpotLight.setPos(worldToCam.matMul(auxSpot).getColumn(0).removeDimensionFromVec(3));
                    return currSpotLight;
                })
                .collect(Collectors.toList());

        LightDataPackage res = new LightDataPackage();

        res.pointLights = new PointLight[pointLightsRes.size()];
        pointLightsRes.toArray(res.pointLights);

        res.spotLights = new SpotLight[spotLightsRes.size()];
        spotLightsRes.toArray(res.spotLights);

        res.directionalLights = new DirectionalLight[directionalLightsRes.size()];
        directionalLightsRes.toArray(res.directionalLights);

        return res;
    }

}