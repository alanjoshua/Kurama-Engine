package Kurama.renderingEngine.defaultRenderPipeline;

import Kurama.Math.Matrix;
import Kurama.Mesh.InstancedUtils;
import Kurama.Mesh.Mesh;
import Kurama.game.Game;
import Kurama.lighting.DirectionalLight;
import Kurama.lighting.SpotLight;
import Kurama.model.AnimatedModel;
import Kurama.model.Model;
import Kurama.renderingEngine.*;
import Kurama.scene.Scene;
import Kurama.shader.ShaderProgram;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL45C.glNamedBufferSubData;

public class ShadowBlock extends RenderPipeline {

    private String shadow_ShaderID = "shadow_shader";
    private ShaderProgram shadow_shader;

    public ShadowBlock(Game game, RenderPipeline parentPipeline, String pipelineID) {
        super(game,parentPipeline, pipelineID);
    }

    @Override
    public void setup(RenderPipelineInput input) {
        shadow_shader = new ShaderProgram(shadow_ShaderID);
        try {
            shadow_shader.createVertexShader("src/main/java/Kurama/renderingEngine/defaultRenderPipeline/shaders/depthDirectionalLightVertexShader.glsl");
            shadow_shader.createFragmentShader("src/main/java/Kurama/renderingEngine/defaultRenderPipeline/shaders/depthDirectionalLightFragmentShader.glsl");
            shadow_shader.link();

            shadow_shader.createUniform("projectionMatrix");
            shadow_shader.createUniform("modelLightViewMatrix");
            shadow_shader.createUniformArray("jointMatrices", DefaultRenderPipeline.MAX_JOINTS);
            shadow_shader.createUniform("isAnimated");
            shadow_shader.createUniform("isInstanced");

        }catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public RenderPipelineOutput render(RenderPipelineInput input) {
        var shadowPackage = calculateShadowMaps(input.scene);
        return new ShadowPackageRenderBlockOutput(shadowPackage);
    }

    public ShadowDepthRenderPackage calculateShadowMaps(Scene scene) {

        DefaultRenderPipeline pipeline = (DefaultRenderPipeline) parentPipeline;
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

                glBindFramebuffer(GL_FRAMEBUFFER, light.shadowMap.depthMapFBO);
                glViewport(0, 0, light.shadowMap.shadowMapWidth, light.shadowMap.shadowMapHeight);
                glClear(GL_DEPTH_BUFFER_BIT);

                depthShaderProgram.setUniform("projectionMatrix", light.shadowProjectionMatrix);

                for (String meshId : scene.shaderblock_mesh_model_map.get(DefaultRenderPipeline.sceneShaderBlockID).keySet()) {

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

                    if (mesh.isInstanced) {
                        depthShaderProgram.setUniform("isInstanced", 1);
                        var inst_mesh = mesh;
                        var models = new ArrayList<Model>();

                        for (String modelId : scene.shaderblock_mesh_model_map.get(DefaultRenderPipeline.sceneShaderBlockID).get(meshId).keySet()) {
                            var m = scene.modelID_model_map.get(modelId);
                            if (m.shouldRender && m.shouldSelfCastShadow) {
                                models.add(m);
                            }
                        }
                        List<List<Model>> chunks;
                        if (mesh.isAnimatedSkeleton) {
                            chunks = InstancedUtils.getRenderChunks(models,
                                    inst_mesh.instanceChunkSize < DefaultRenderPipeline.MAX_INSTANCED_SKELETAL_MESHES ?
                                            inst_mesh.instanceChunkSize : DefaultRenderPipeline.MAX_INSTANCED_SKELETAL_MESHES);
                            depthShaderProgram.setUniform("isAnimated", 1);
                        } else {
                            chunks = InstancedUtils.getRenderChunks(models, inst_mesh.instanceChunkSize);
                            depthShaderProgram.setUniform("isAnimated", 0);
                        }

                        for (var chunk : chunks) {
                            inst_mesh.instanceDataBuffer.clear();

                            int modelCount = 0;
                            for (Model m : chunk) {

                                if (mesh.isAnimatedSkeleton) {
                                    var anim = (AnimatedModel) m;
                                    for (int j = 0; j < anim.currentAnimation.numJoints; j++) {
                                        Matrix jointMat;
                                        jointMat = anim.currentJointTransformations.get(j);
//                                }
                                        FloatBuffer temp = jointMat.getAsFloatBuffer();
//                                TODO: Change this to persistent map
                                        glNamedBufferSubData(pipeline.jointsInstancedBufferID,
                                                ((modelCount * DefaultRenderPipeline.MAX_JOINTS) + j) *
                                                        DefaultRenderPipeline.MATRIX_SIZE_BYTES, temp);
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
                        for (String modelId : scene.shaderblock_mesh_model_map.get(DefaultRenderPipeline.sceneShaderBlockID).get(meshId).keySet()) {
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

                            if (m.shouldSelfCastShadow && m.shouldRender) {
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

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        for(int i =0;i < scene.spotLights.size();i++) {

            SpotLight light = scene.spotLights.get(i);
            Matrix worldToLight = light.getWorldToObject();
            worldToSpotLights.add(worldToLight);

            if(light.doesProduceShadow) {

                Matrix projMatrix = light.shadowProjectionMatrix;
                depthShaderProgram.setUniform("projectionMatrix", projMatrix);

                glBindFramebuffer(GL_FRAMEBUFFER, light.shadowMap.depthMapFBO);
                glViewport(0, 0, light.shadowMap.shadowMapWidth, light.shadowMap.shadowMapHeight);
                glClear(GL_DEPTH_BUFFER_BIT);

                for (String meshId : scene.shaderblock_mesh_model_map.get(DefaultRenderPipeline.sceneShaderBlockID).keySet()) {
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

                    if (mesh.isInstanced) {
                        depthShaderProgram.setUniform("isInstanced", 1);
                        var inst_mesh = mesh;
                        var models = new ArrayList<Model>();

                        for (String modelId : scene.shaderblock_mesh_model_map.get(DefaultRenderPipeline.sceneShaderBlockID).get(meshId).keySet()) {
                            var m = scene.modelID_model_map.get(modelId);
                            if (m.shouldRender && m.shouldSelfCastShadow) {
                                models.add(m);
                            }
                        }
                        List<List<Model>> chunks;
                        if (mesh.isAnimatedSkeleton) {
                            chunks = InstancedUtils.getRenderChunks(models,
                                    inst_mesh.instanceChunkSize < DefaultRenderPipeline.MAX_INSTANCED_SKELETAL_MESHES ?
                                            inst_mesh.instanceChunkSize : DefaultRenderPipeline.MAX_INSTANCED_SKELETAL_MESHES);
                            depthShaderProgram.setUniform("isAnimated", 1);
                        } else {
                            chunks = InstancedUtils.getRenderChunks(models, inst_mesh.instanceChunkSize);
                            depthShaderProgram.setUniform("isAnimated", 0);
                        }

                        for (var chunk : chunks) {
                            inst_mesh.instanceDataBuffer.clear();

                            int modelCount = 0;
                            for (Model m : chunk) {

                                if (mesh.isAnimatedSkeleton) {
                                    var anim = (AnimatedModel) m;
                                    for (int j = 0; j < anim.currentAnimation.numJoints; j++) {
                                        Matrix jointMat;
                                        jointMat = anim.currentJointTransformations.get(j);
//                                }
                                        FloatBuffer temp = jointMat.getAsFloatBuffer();
//                                TODO: Change this to persistent map
                                        glNamedBufferSubData(pipeline.jointsInstancedBufferID,
                                                ((modelCount * DefaultRenderPipeline.MAX_JOINTS) + j)
                                                        * DefaultRenderPipeline.MATRIX_SIZE_BYTES, temp);
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
                        for (String modelId : scene.shaderblock_mesh_model_map.get(DefaultRenderPipeline.sceneShaderBlockID).get(meshId).keySet()) {
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

                            if (m.shouldSelfCastShadow && m.shouldRender) {
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

    @Override
    public void cleanUp() {

    }
}
