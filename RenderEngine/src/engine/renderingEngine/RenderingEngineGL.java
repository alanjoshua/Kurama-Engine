package engine.renderingEngine;

import engine.DataStructure.Mesh.Mesh;
import engine.scene.Scene;
import engine.lighting.SpotLight;
import engine.model.HUD;
import engine.Math.Matrix;
import engine.Math.Vector;
import engine.camera.Camera;
import engine.lighting.DirectionalLight;
import engine.model.MeshBuilder;
import engine.utils.Utils;
import engine.shader.ShaderProgram;
import engine.game.Game;
import engine.model.Model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

public class RenderingEngineGL extends RenderingEngine {

    public Map<String, ShaderProgram> shaderID_shader_map = new HashMap<>();
    protected Mesh axes;

    public String sceneShaderID = "sceneShader";
    public String hudShaderID = "hudShader";
    public String skyboxShaderID = "skyboxShader";
    public String depthShaderID = "depthShader";

    public static int MAX_DIRECTIONAL_LIGHTS = 5;
    public static int MAX_SPOTLIGHTS = 10;
    public static int MAX_POINTLIGHTS = 10;

    public void init() {
        axes = MeshBuilder.buildAxes();

        glEnable(GL_DEPTH_TEST);    //Enables depth testing

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        setupSceneShader();
        depthShader();
        setupHUDShader();
        setupSkybox();
    }

    public void setupSceneShader() {
        try {
            ShaderProgram sceneShaderProgram = new ShaderProgram(sceneShaderID);
            shaderID_shader_map.put(sceneShaderID, sceneShaderProgram);

            sceneShaderProgram.createVertexShader(Utils.loadResourceAsString("/Shaders/SceneVertexShader.glsl"));
            sceneShaderProgram.createFragmentShader(Utils.loadResourceAsString("/Shaders/SceneFragmentShader.glsl"));
            sceneShaderProgram.link();

            sceneShaderProgram.createUniform("projectionMatrix");

            sceneShaderProgram.createUniformArray("modelLightViewMatrix",MAX_DIRECTIONAL_LIGHTS);
            sceneShaderProgram.createUniformArray("modelSpotLightViewMatrix",MAX_SPOTLIGHTS);
            sceneShaderProgram.createUniformArray("directionalShadowMaps",MAX_DIRECTIONAL_LIGHTS);
            sceneShaderProgram.createUniformArray("spotLightShadowMaps",MAX_SPOTLIGHTS);
            sceneShaderProgram.createUniform("modelViewMatrix");
            sceneShaderProgram.createUniform("numDirectionalLights");
            sceneShaderProgram.createUniform("numberOfSpotLights");
            sceneShaderProgram.createMaterialListUniform("materials","mat_textures","mat_normalMaps","mat_diffuseMaps","mat_specularMaps",26);

            sceneShaderProgram.createUniform("ambientLight");

            sceneShaderProgram.createPointLightListUniform("pointLights",MAX_POINTLIGHTS);
            sceneShaderProgram.createDirectionalLightListUniform("directionalLights",MAX_DIRECTIONAL_LIGHTS);
            sceneShaderProgram.createSpotLightListUniform("spotLights",MAX_SPOTLIGHTS);

            sceneShaderProgram.createFogUniform("fog");
            sceneShaderProgram.createUniform("allDirectionalLightStatic");

            sceneShaderProgram.createUniformArray("directionalLightOrthoMatrix", 5);
            sceneShaderProgram.createUniformArray("spotlightPerspMatrix", 5);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void depthShader() {
        ShaderProgram depthShaderProgram = new ShaderProgram(depthShaderID);
        shaderID_shader_map.put(depthShaderID, depthShaderProgram);

        depthShaderProgram.createVertexShader(Utils.loadResourceAsString("/Shaders/depthDirectionalLightVertexShader.glsl"));
        depthShaderProgram.createFragmentShader(Utils.loadResourceAsString("/Shaders/depthDirectionalLightFragmentShader.glsl"));
        depthShaderProgram.link();

        depthShaderProgram.createUniform("projectionMatrix");
        depthShaderProgram.createUniform("modelLightViewMatrix");
//        directionalLightDepthShaderProgram.createUniform("nearZ");
//        directionalLightDepthShaderProgram.createUniform("farZ");
//        directionalLightDepthShaderProgram.createUniform("shouldLinearizeDepth");
    }

    public void setupHUDShader() {
        try {
        ShaderProgram hudShaderProgram = new ShaderProgram(hudShaderID);
        shaderID_shader_map.put(hudShaderID, hudShaderProgram);

        hudShaderProgram.createVertexShader(Utils.loadResourceAsString("/Shaders/HUDVertexShader.glsl"));
        hudShaderProgram.createFragmentShader(Utils.loadResourceAsString("/Shaders/HUDFragmentShader.glsl"));
        hudShaderProgram.link();

        // Create uniforms for Orthographic-model projection matrix and base colour
        hudShaderProgram.createUniform("texture_sampler");
        hudShaderProgram.createUniform("projModelMatrix");
        hudShaderProgram.createUniform("color");
        hudShaderProgram.createUniform("shouldGreyScale");
        hudShaderProgram.createUniform("shouldLinearizeDepth");

        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void setupSkybox() {
        try {
            ShaderProgram skyBoxShaderProgram = new ShaderProgram(skyboxShaderID);
            shaderID_shader_map.put(skyboxShaderID, skyBoxShaderProgram);

            skyBoxShaderProgram.createVertexShader(Utils.loadResourceAsString("/Shaders/SkyBoxVertexShader.glsl"));
            skyBoxShaderProgram.createFragmentShader(Utils.loadResourceAsString("/Shaders/SkyBoxFragmentShader.glsl"));
            skyBoxShaderProgram.link();

            skyBoxShaderProgram.createUniform("projectionMatrix");
            skyBoxShaderProgram.createUniform("modelViewMatrix");
            skyBoxShaderProgram.createUniform("texture_sampler");
            skyBoxShaderProgram.createUniform("ambientLight");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public RenderingEngineGL(Game game) {
        super(game);
    }

    public void enableModelOutline() {
        glPolygonMode(GL_FRONT_AND_BACK,GL_LINE);
    }

    public void enableModelFill() {
        glPolygonMode(GL_FRONT_AND_BACK,GL_TRIANGLES);
    }

    public void clear() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    public void render(Scene scene, Camera camera) {
        ShadowDepthRenderPackage shadowPackage =  renderDepthMap(scene);
        glViewport(0,0,game.getDisplay().getWidth(),game.getDisplay().getHeight());
        clear();
        renderScene(scene,camera,shadowPackage);
        renderSkyBox(scene,camera);
        renderHUD(scene);
    }

    public ShaderProgram getShader(String shaderID) {
        return shaderID_shader_map.get(shaderID);
    }

    public void renderScene(Scene scene, Camera camera, ShadowDepthRenderPackage shadowPackage) {
        ShaderProgram sceneShaderProgram = getShader(sceneShaderID);
        sceneShaderProgram.bind();

        Matrix worldToCam = camera.getWorldToCam();
        Matrix projectionMatrix = camera.getPerspectiveProjectionMatrix();

        sceneShaderProgram.setUniform("projectionMatrix",projectionMatrix);

        sceneShaderProgram.setUniform("ambientLight",scene.ambientLight);

        LightDataPackage lights = processLights(scene.pointLights, scene.spotLights, scene.directionalLights, worldToCam);
        sceneShaderProgram.setUniform("spotLights",lights.spotLights);
        sceneShaderProgram.setUniform("pointLights",lights.pointLights);
        sceneShaderProgram.setUniform("directionalLights",lights.directionalLights);
        sceneShaderProgram.setUniform("numDirectionalLights",scene.directionalLights.size());
        sceneShaderProgram.setUniform("numberOfSpotLights",scene.spotLights.size());
        sceneShaderProgram.setUniform("fog", scene.fog);

        Vector c = new Vector(3,0);
        for(int i = 0;i < scene.directionalLights.size(); i++) {
            DirectionalLight l = scene.directionalLights.get(i);
            c = c.add(l.color.scalarMul(l.intensity));
            sceneShaderProgram.setUniform("directionalLightOrthoMatrix["+i+"]", l.shadowProjectionMatrix);
        }
        sceneShaderProgram.setUniform("allDirectionalLightStatic", c);

        for(int i = 0;i < scene.spotLights.size(); i++) {
            SpotLight l = scene.spotLights.get(i);
            sceneShaderProgram.setUniform("spotlightPerspMatrix["+i+"]", l.shadowProjectionMatrix);
        }

        Map<Mesh, List<Model>> accessoryModels = new HashMap<>();
        //int offset = 0;
        int offset = sceneShaderProgram.setAndActivateDirectionalShadowMaps("directionalShadowMaps", scene.directionalLights,0);
        offset = sceneShaderProgram.setAndActivateSpotLightShadowMaps("spotLightShadowMaps", scene.spotLights, offset);

        for(String meshId :scene.shader_mesh_model_map.get(sceneShaderID).keySet()) {
            Mesh mesh = scene.meshID_mesh_map.get(meshId);
            sceneShaderProgram.setAndActivateMaterials("materials","mat_textures",
                    "mat_normalMaps","mat_diffuseMaps","mat_specularMaps",mesh.materials,offset);
//            sceneShaderProgram.setUniform("directionalShadowMaps[0]",offset);
//            glActiveTexture(offset+GL_TEXTURE0);
//            glBindTexture(GL_TEXTURE_2D, lights.directionalLights[0].shadowMap.depthMap.getId());

            mesh.initRender();

            for(String modelId : scene.shader_mesh_model_map.get(sceneShaderID).get(meshId).keySet()) {
                Model model = scene.modelID_model_map.get(modelId);

                if (model.shouldRender) {
                    Matrix objectToWorld = model.getObjectToWorldMatrix();
                    sceneShaderProgram.setUniform("modelViewMatrix", worldToCam.matMul(objectToWorld));
                    for (int i = 0; i < scene.directionalLights.size(); i++) {
                        sceneShaderProgram.setUniform("modelLightViewMatrix[" + i + "]",
                                shadowPackage.worldToDirectionalLights.get(i).matMul(objectToWorld));
                    }
                    for (int i = 0; i < scene.spotLights.size(); i++) {
                        sceneShaderProgram.setUniform("modelSpotLightViewMatrix[" + i + "]",
                                shadowPackage.worldToSpotLights.get(i).matMul(objectToWorld));
                    }
                    mesh.render();

                    if (model.shouldShowCollisionBox && model.getBoundingBox() != null) {
                        List<Model> l = accessoryModels.get(model.getBoundingBox());
                        if (l == null) {
                            l = new ArrayList<>();
                            accessoryModels.put(model.getBoundingBox(), l);
                        }
                        l.add(model);
                    }
                    if (model.shouldShowAxes && axes != null) {
                        List<Model> l = accessoryModels.get(axes);
                        if (l == null) {
                            l = new ArrayList<>();
                            accessoryModels.put(axes, l);
                        }
                        l.add(model);
                    }
                }
            }

            mesh.endRender();
            //int shadowMapInd = mesh.endRender();
            //glBindTexture(GL_TEXTURE_2D,shadowMapInd);
        }

        for(Mesh mesh:accessoryModels.keySet()) {
            sceneShaderProgram.setAndActivateMaterials("materials","mat_textures",
                    "mat_normalMaps","mat_diffuseMaps","mat_specularMaps",
                    mesh.materials,offset);

            mesh.initRender();
            for(Model model:accessoryModels.get(mesh)) {

                if (model.shouldRender) {
                    Matrix objectToWorld = model.getObjectToWorldMatrix();
                    sceneShaderProgram.setUniform("modelViewMatrix", worldToCam.matMul(objectToWorld));
                    for (int i = 0; i < scene.directionalLights.size(); i++) {
                        sceneShaderProgram.setUniform("modelLightViewMatrix[" + i + "]", shadowPackage.worldToDirectionalLights.get(i).matMul(objectToWorld));
                    }
                    for (int i = 0; i < scene.spotLights.size(); i++) {
                        sceneShaderProgram.setUniform("modelSpotLightViewMatrix[" + i + "]", shadowPackage.worldToSpotLights.get(i).matMul(objectToWorld));
                    }
                    mesh.render();
                }

            }
            mesh.endRender();
        }

        sceneShaderProgram.unbind();
    }

    public void renderHUD(Scene scene) {

        if(scene.hud == null) {
            return;
        }

        Matrix ortho = Matrix.buildOrtho2D(0, game.getDisplay().getWidth(), game.getDisplay().getHeight(), 0);

        ShaderProgram hudShaderProgram = getShader(hudShaderID);
        hudShaderProgram.bind();
        hudShaderProgram.setUniform("texture_sampler", 0);

        for(String meshId :scene.shader_mesh_model_map.get(hudShaderID).keySet()) {
            Mesh mesh = scene.meshID_mesh_map.get(meshId);

//            mesh.initRender();

            for(String modelId : scene.shader_mesh_model_map.get(hudShaderID).get(meshId).keySet()) {
                Model m = scene.modelID_model_map.get(modelId);

                if (m.shouldRender) {
                    // Set orthographic and model matrix for this HUD item
                    Matrix projModelMatrix = ortho.matMul((m.getObjectToWorldMatrix()));
                    hudShaderProgram.setUniform("projModelMatrix", projModelMatrix);
                    hudShaderProgram.setUniform("color", m.mesh.materials.get(0).ambientColor);

                    hudShaderProgram.setUniform("shouldGreyScale", m.shouldGreyScale ? 1 : 0);
                    hudShaderProgram.setUniform("shouldLinearizeDepth", m.shouldLinearizeDepthInHUD ? 1 : 0);

                    m.mesh.initToEndFullRender(0);
                }
            }

//            mesh.endRender();
        }

        hudShaderProgram.unbind();

    }

    public void renderSkyBox(Scene scene, Camera camera) {

        if(scene.skybox == null) {
            return;
        }

        ShaderProgram skyBoxShaderProgram = getShader(skyboxShaderID);
        skyBoxShaderProgram.bind();

        if (scene.skybox.shouldRender) {
            skyBoxShaderProgram.setUniform("texture_sampler", 0);

            // Update projection Matrix
            Matrix projectionMatrix = camera.getPerspectiveProjectionMatrix();
            skyBoxShaderProgram.setUniform("projectionMatrix", projectionMatrix);

            Model skyBox = scene.skybox;
            skyBox.setPos(camera.getPos());
            Matrix modelViewMatrix = camera.getWorldToCam().matMul(skyBox.getObjectToWorldMatrix());
            skyBoxShaderProgram.setUniform("modelViewMatrix", modelViewMatrix);
            skyBoxShaderProgram.setUniform("ambientLight", skyBox.mesh.materials.get(0).ambientColor);

            scene.skybox.getMesh().initToEndFullRender(0);
        }
        skyBoxShaderProgram.unbind();
    }

    public ShadowDepthRenderPackage renderDepthMap(Scene scene) {

        glCullFace(GL_BACK);

        ShaderProgram depthShaderProgram = getShader(depthShaderID);
        depthShaderProgram.bind();
        List<Matrix> worldToDirectionalLights = new ArrayList<>();
        List<Matrix> worldToSpotLights = new ArrayList<>();

        for(int i =0;i < scene.directionalLights.size();i++) {
            DirectionalLight light = scene.directionalLights.get(i);
            glViewport(0,0, light.shadowMap.shadowMapWidth, light.shadowMap.shadowMapHeight);
            glBindFramebuffer(GL_FRAMEBUFFER, light.shadowMap.depthMapFBO);
            glClear(GL_DEPTH_BUFFER_BIT);

            depthShaderProgram.setUniform("projectionMatrix", light.shadowProjectionMatrix);

            Matrix worldToLight = light.getWorldToObject();
            worldToDirectionalLights.add(worldToLight);

            for(String meshId :scene.shader_mesh_model_map.get(sceneShaderID).keySet()) {
                Mesh mesh = scene.meshID_mesh_map.get(meshId);

                mesh.initRender();

                for(String modelId : scene.shader_mesh_model_map.get(sceneShaderID).get(meshId).keySet()) {
                    Model m = scene.modelID_model_map.get(modelId);

                    if (m.shouldCastShadow && m.shouldRender) {
                        Matrix modelLightViewMatrix = worldToLight.matMul(m.getObjectToWorldMatrix());
                        depthShaderProgram.setUniform("modelLightViewMatrix", modelLightViewMatrix);
                        mesh.render();
                    }
                }
                mesh.endRender();
            }
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
        }

        for(int i =0;i < scene.spotLights.size();i++) {
            SpotLight light = scene.spotLights.get(i);

            Matrix projMatrix = light.shadowProjectionMatrix;
            depthShaderProgram.setUniform("projectionMatrix", projMatrix);
            glViewport(0,0, light.shadowMap.shadowMapWidth, light.shadowMap.shadowMapHeight);
            glBindFramebuffer(GL_FRAMEBUFFER, light.shadowMap.depthMapFBO);
            glClear(GL_DEPTH_BUFFER_BIT);

            Matrix worldToLight = light.getWorldToObject();
            worldToSpotLights.add(worldToLight);

            for(String meshId :scene.shader_mesh_model_map.get(sceneShaderID).keySet()) {
                Mesh mesh = scene.meshID_mesh_map.get(meshId);

                mesh.initRender();
                for(String modelId : scene.shader_mesh_model_map.get(sceneShaderID).get(meshId).keySet()) {
                    Model m = scene.modelID_model_map.get(modelId);

                    if (m.shouldCastShadow) {
                        Matrix modelLightViewMatrix = worldToLight.matMul(m.getObjectToWorldMatrix());
                        depthShaderProgram.setUniform("modelLightViewMatrix", modelLightViewMatrix);
                        mesh.render();
                    }
                }
                mesh.endRender();
            }
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
        }

        depthShaderProgram.unbind();
        glCullFace(GL_BACK);
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

    public void cleanUp() {
        for (ShaderProgram shader: shaderID_shader_map.values()) {
            if (shader != null) {
                shader.cleanUp();
            }
        }
    }

}

