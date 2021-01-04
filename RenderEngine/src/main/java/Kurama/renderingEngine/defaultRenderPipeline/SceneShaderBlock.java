package Kurama.renderingEngine.defaultRenderPipeline;

import Kurama.Math.Matrix;
import Kurama.Math.Vector;
import Kurama.Mesh.InstancedMesh;
import Kurama.Mesh.Material;
import Kurama.Mesh.Mesh;
import Kurama.camera.Camera;
import Kurama.lighting.DirectionalLight;
import Kurama.lighting.PointLight;
import Kurama.lighting.SpotLight;
import Kurama.model.AnimatedModel;
import Kurama.model.Model;
import Kurama.renderingEngine.*;
import Kurama.scene.Scene;
import Kurama.shader.ShaderProgram;
import Kurama.utils.Logger;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL45.glNamedBufferSubData;

//import static org.lwjgl.opengl.GL30C.*;

public class SceneShaderBlock extends Kurama.renderingEngine.RenderBlock {

    private String scene_shader_id = "scene_shader";
    private ShaderProgram scene_shader;
    private DefaultRenderPipeline pipeline;

    public SceneShaderBlock(String id, RenderPipeline pipeline) {
        super(id, pipeline);
        this.pipeline = (DefaultRenderPipeline)pipeline;
    }

    @Override
    public void setup(RenderBlockInput input) {
        setupSceneShader();
    }

    @Override
    public RenderBlockOutput render(RenderBlockInput input) {

        CurrentCameraBlockInput inp = (CurrentCameraBlockInput) input;
        ShadowPackageRenderBlockOutput out = (ShadowPackageRenderBlockOutput) input.previousOutput;

        var camera = inp.camera;
        var shadowPackage = out.shadowPackage;

        renderScene(input.scene, camera, shadowPackage);

        return null;
    }

    @Override
    public void cleanUp() {
        scene_shader.cleanUp();
    }

    public void setupSceneShader() {

        scene_shader = new ShaderProgram(scene_shader_id);

        try {

            scene_shader.createVertexShader("src/main/java/Kurama/renderingEngine/defaultRenderPipeline/shaders/SceneVertexShader.glsl");
            scene_shader.createFragmentShader("src/main/java/Kurama/renderingEngine/defaultRenderPipeline/shaders/SceneFragmentShader.glsl");
            scene_shader.link();

            scene_shader.createUniform("projectionMatrix");

//            scene_shader.createUniformArray("modelLightViewMatrix",MAX_DIRECTIONAL_LIGHTS);
//            scene_shader.createUniformArray("modelSpotLightViewMatrix",MAX_SPOTLIGHTS);
            scene_shader.createUniformArray("directionalShadowMaps", DefaultRenderPipeline.MAX_DIRECTIONAL_LIGHTS);
            scene_shader.createUniformArray("spotLightShadowMaps", DefaultRenderPipeline.MAX_SPOTLIGHTS);
            scene_shader.createUniform("numDirectionalLights");
            scene_shader.createUniform("numberOfSpotLights");
            scene_shader.createUniform("isAnimated");
            scene_shader.createMaterialListUniform("materials","mat_textures","mat_normalMaps","mat_diffuseMaps","mat_specularMaps",26);
            scene_shader.createUniform("isInstanced");
            scene_shader.createUniform("ambientLight");

            scene_shader.createPointLightListUniform("pointLights",DefaultRenderPipeline.MAX_POINTLIGHTS);
            scene_shader.createDirectionalLightListUniform("directionalLights",DefaultRenderPipeline.MAX_DIRECTIONAL_LIGHTS);
            scene_shader.createSpotLightListUniform("spotLights",DefaultRenderPipeline.MAX_SPOTLIGHTS);

            scene_shader.createFogUniform("fog");
            scene_shader.createUniform("allDirectionalLightStatic");

            scene_shader.createUniformArray("worldToDirectionalLightMatrix", DefaultRenderPipeline.MAX_DIRECTIONAL_LIGHTS);
            scene_shader.createUniformArray("worldToSpotlightMatrix", DefaultRenderPipeline.MAX_SPOTLIGHTS);
            scene_shader.createUniformArray("jointMatrices", DefaultRenderPipeline.MAX_JOINTS);
            scene_shader.createUniform("modelToWorldMatrix");
            scene_shader.createUniform("worldToCam");

            scene_shader.createUniform("materialsGlobalLoc");
//            scene_shader.createUniform("materialsAtlas");

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

    public void renderScene(Scene scene, Camera camera, ShadowDepthRenderPackage shadowPackage) {

        DefaultRenderPipeline pipeline = (DefaultRenderPipeline) renderPipeline;
        boolean curShouldCull = true;
        int currCull = GL_BACK;

        ShaderProgram sceneShaderProgram = scene_shader;
        sceneShaderProgram.bind();

        Matrix worldToCam = camera.getWorldToCam();
        Matrix projectionMatrix = camera.getPerspectiveProjectionMatrix();

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
                    if(model.shouldRender && model.isInsideFrustum) {
                        models.add(model);
                    }
                }

                var inst_mesh = (InstancedMesh) mesh;
                List<List<Model>> chunks;
                if(mesh.isAnimatedSkeleton) {
                    chunks = InstancedMesh.getRenderChunks(models,
                            inst_mesh.instanceChunkSize < DefaultRenderPipeline.MAX_INSTANCED_SKELETAL_MESHES ?
                                    inst_mesh.instanceChunkSize: DefaultRenderPipeline.MAX_INSTANCED_SKELETAL_MESHES);
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
                            for (int i = 0; i < anim.currentAnimation.numJoints; i++) {
                                Matrix jointMat;
                                jointMat = anim.currentJointTransformations.get(i);
//                                }
                                FloatBuffer temp = jointMat.getAsFloatBuffer();
//                                TODO: Change this to persistent map
                                glNamedBufferSubData(pipeline.jointsInstancedBufferID,
                                        ((modelCount * DefaultRenderPipeline.MAX_JOINTS) + i) * DefaultRenderPipeline.MATRIX_SIZE_BYTES, temp);
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
                            try {
                                matsGlobalLoc.setDataElement(i, model.materials.get(meshId).get(i).globalSceneID);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                                System.exit(1);
                            }
                        }

                        scene_shader.setUniform("materialsGlobalLoc", matsGlobalLoc);
                        Matrix objectToWorld = model.getObjectToWorldMatrix();
                        scene_shader.setUniform("modelToWorldMatrix", objectToWorld);

                        pipeline.render(mesh);
                    }
                }
            }

            pipeline.endRender(mesh);
        }

        sceneShaderProgram.unbind();
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