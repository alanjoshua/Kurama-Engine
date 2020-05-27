package engine.renderingEngine;

import engine.DataStructure.Mesh.Mesh;
import engine.DataStructure.Scene;
import engine.model.HUD;
import engine.Math.Matrix;
import engine.Math.Vector;
import engine.camera.Camera;
import engine.display.DisplayLWJGL;
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

    public ShaderProgram sceneShaderProgram;
    public ShaderProgram hudShaderProgram;
    public ShaderProgram skyBoxShaderProgram;
    public ShaderProgram directionalLightDepthShaderProgram;
    //public List<ShadowMap> directionalShadowMaps;
    protected Mesh axes;
    public Matrix ortho = Matrix.buildOrthographicProjectionMatrix(1,-700,100,-100,-100,100);
    public List<Matrix> worldToDirectionalLights;

    public void init() {
        axes = MeshBuilder.buildAxes();
        worldToDirectionalLights = new ArrayList<>();

        glEnable(GL_DEPTH_TEST);    //Enables depth testing

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        setupSceneShader(game.scene);
        setupDirectionalLightDepthShader();
        setupHUDShader();
        setupSkybox();

    }

//    public void setupShadowMaps(Scene scene) {
////        directionalShadowMaps = new ArrayList<>();
////        for(int i = 0;i < scene.directionalLights.size();i++) {
////            ShadowMap temp =
////            directionalShadowMaps.add(temp);
////        }
//    }

    public void setupSceneShader(Scene scene) {
        try {
            sceneShaderProgram = new ShaderProgram();
            sceneShaderProgram.createVertexShader(Utils.loadResourceAsString("/Shaders/SceneVertexShader.vs"));
            sceneShaderProgram.createFragmentShader(Utils.loadResourceAsString("/Shaders/SceneFragmentShader.fs"));
            sceneShaderProgram.link();

            sceneShaderProgram.createUniform("projectionMatrix");
            sceneShaderProgram.createUniform("orthoProjectionMatrix");
            sceneShaderProgram.createUniformArray("modelLightViewMatrix",5);
            sceneShaderProgram.createUniformArray("directionalShadowMaps",5);
            sceneShaderProgram.createUniform("modelViewMatrix");
            sceneShaderProgram.createUniform("numDirectionalLights");
            sceneShaderProgram.createMaterialListUniform("materials","mat_textures","mat_normalMaps","mat_diffuseMaps","mat_specularMaps",10);

            sceneShaderProgram.createUniform("ambientLight");

            sceneShaderProgram.createPointLightListUniform("pointLights",scene.pointLights.size());
            sceneShaderProgram.createDirectionalLightListUniform("directionalLights",scene.directionalLights.size());
            sceneShaderProgram.createSpotLightListUniform("spotLights",scene.spotLights.size());

            sceneShaderProgram.createFogUniform("fog");
            sceneShaderProgram.createUniform("allDirectionalLightStatic");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setupDirectionalLightDepthShader() {
        directionalLightDepthShaderProgram = new ShaderProgram();
        directionalLightDepthShaderProgram.createVertexShader(Utils.loadResourceAsString("/shaders/depthDirectionalLightVertexShader.vs"));
        directionalLightDepthShaderProgram.createFragmentShader(Utils.loadResourceAsString("/shaders/depthDirectionalLightFragmentShader.fs"));
        directionalLightDepthShaderProgram.link();

        directionalLightDepthShaderProgram.createUniform("orthoProjectionMatrix");
        directionalLightDepthShaderProgram.createUniform("modelLightViewMatrix");
    }

    public void setupHUDShader() {
        try {
        hudShaderProgram = new ShaderProgram();
        hudShaderProgram.createVertexShader(Utils.loadResourceAsString("/shaders/HUDVertexShader.vs"));
        hudShaderProgram.createFragmentShader(Utils.loadResourceAsString("/shaders/HUDFragmentShader.fs"));
        hudShaderProgram.link();

        // Create uniforms for Orthographic-model projection matrix and base colour
        hudShaderProgram.createUniform("texture_sampler");
        hudShaderProgram.createUniform("projModelMatrix");
        hudShaderProgram.createUniform("color");

        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void setupSkybox() {
        try {
            skyBoxShaderProgram = new ShaderProgram();
            skyBoxShaderProgram.createVertexShader(Utils.loadResourceAsString("/shaders/SkyBoxVertexShader.vs"));
            skyBoxShaderProgram.createFragmentShader(Utils.loadResourceAsString("/shaders/SkyBoxFragmentShader.fs"));
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

    public void render(Scene scene, HUD hud, Camera camera) {
        renderDepthMap(((DisplayLWJGL)game.getDisplay()).getWindow(),camera,scene,hud);
        scene.models.get(1).mesh.materials.get(0).texture = scene.directionalLights.get(0).shadowMap.depthMap;
        glViewport(0,0,game.getDisplay().getWidth(),game.getDisplay().getHeight());
        clear();
        renderScene(scene,camera);
        renderSkyBox(scene,camera);
        renderHUD(hud);
    }

    public void renderScene(Scene scene, Camera camera) {
        sceneShaderProgram.bind();

        Matrix worldToCam = camera.getWorldToCam();
        Matrix projectionMatrix = camera.getPerspectiveProjectionMatrix();

        sceneShaderProgram.setUniform("projectionMatrix",projectionMatrix);
        sceneShaderProgram.setUniform("orthoProjectionMatrix",ortho);

        sceneShaderProgram.setUniform("ambientLight",scene.ambientLight);
        //sceneShaderProgram.setUniform("specularPower",scene.specularPower);

        LightDataPackage lights = processLights(scene.pointLights, scene.spotLights, scene.directionalLights, worldToCam);
        sceneShaderProgram.setUniform("spotLights",lights.spotLights);
        sceneShaderProgram.setUniform("pointLights",lights.pointLights);
        sceneShaderProgram.setUniform("directionalLights",lights.directionalLights);
        sceneShaderProgram.setUniform("numDirectionalLights",scene.directionalLights.size());

        sceneShaderProgram.setUniform("fog", scene.fog);

        Vector c = new Vector(3,0);
        for(DirectionalLight l:scene.directionalLights) {
            c = c.add(l.color.scalarMul(l.intensity));
        }
        sceneShaderProgram.setUniform("allDirectionalLightStatic", c);

        Map<Mesh, List<Model>> accessoryModels = new HashMap<>();
        //int offset = 0;
        int offset = sceneShaderProgram.setAndActivateShadowMaps("directionalShadowMaps", scene.directionalLights,0);

        for(Mesh mesh:scene.modelMap.keySet()) {
            sceneShaderProgram.setAndActivateMaterials("materials","mat_textures","mat_normalMaps","mat_diffuseMaps","mat_specularMaps",mesh.materials,offset);
//            sceneShaderProgram.setUniform("directionalShadowMaps[0]",offset);
//            glActiveTexture(offset+GL_TEXTURE0);
//            glBindTexture(GL_TEXTURE_2D, lights.directionalLights[0].shadowMap.depthMap.getId());

            mesh.initRender();

            for(Model model:scene.modelMap.get(mesh)) {
                Matrix objectToWorld =  model.getObjectToWorldMatrix();
                sceneShaderProgram.setUniform("modelViewMatrix",worldToCam.matMul(objectToWorld));
                for(int i = 0;i < scene.directionalLights.size();i++) {
                    sceneShaderProgram.setUniform("modelLightViewMatrix["+i+"]", worldToDirectionalLights.get(i).matMul(objectToWorld));
                }
                mesh.render();

                if(model.shouldShowCollisionBox && model.getBoundingBox() != null) {
                    List<Model> l = accessoryModels.get(model.getBoundingBox());
                    if(l == null) {
                        l = new ArrayList<>();
                        accessoryModels.put(model.getBoundingBox(),l);
                    }
                    l.add(model);
                }
                if(model.shouldShowAxes && axes != null) {
                    List<Model> l = accessoryModels.get(axes);
                    if(l == null) {
                        l = new ArrayList<>();
                        accessoryModels.put(axes,l);
                    }
                    l.add(model);
                }
            }
            mesh.endRender();
            //int shadowMapInd = mesh.endRender();
            //glBindTexture(GL_TEXTURE_2D,shadowMapInd);
        }

        for(Mesh mesh:accessoryModels.keySet()) {
            sceneShaderProgram.setAndActivateMaterials("materials","mat_textures","mat_normalMaps","mat_diffuseMaps","mat_specularMaps",mesh.materials,offset);

            mesh.initRender();
            for(Model model:accessoryModels.get(mesh)) {
                Matrix objectToWorld =  model.getObjectToWorldMatrix();
                sceneShaderProgram.setUniform("modelViewMatrix",worldToCam.matMul(objectToWorld));
                for(int i = 0;i < scene.directionalLights.size();i++){
                    sceneShaderProgram.setUniform("modelLightViewMatrix["+i+"]", worldToDirectionalLights.get(i).matMul(objectToWorld));
                }
               // sceneShaderProgram.setUniform("modelLightViewMatrix", worldToDirectionalLights.matMul(objectToWorld));
                mesh.render();
            }
            mesh.endRender();
        }

        sceneShaderProgram.unbind();
    }

    public void renderHUD(HUD hud) {

        if(hud == null) {
            return;
        }

        hudShaderProgram.bind();
        hudShaderProgram.setUniform("texture_sampler", 0);

        for(Model m: hud.hudElements) {
            Matrix ortho = Matrix.buildOrtho2D(0, game.getDisplay().getWidth(), game.getDisplay().getHeight(), 0);

            // Set orthographic and model matrix for this HUD item
            Matrix projModelMatrix = ortho.matMul((m.getObjectToWorldMatrix()));
            hudShaderProgram.setUniform("projModelMatrix", projModelMatrix);
            hudShaderProgram.setUniform("color", m.mesh.materials.get(0).ambientColor);

            m.mesh.initToEndFullRender(0);
        }

        hudShaderProgram.unbind();

    }

    public void renderSkyBox(Scene scene, Camera camera) {

        if(scene.skybox == null) {
            return;
        }

        skyBoxShaderProgram.bind();

        skyBoxShaderProgram.setUniform("texture_sampler", 0);

        // Update projection Matrix
        Matrix projectionMatrix = camera.getPerspectiveProjectionMatrix();
        skyBoxShaderProgram.setUniform("projectionMatrix", projectionMatrix);

        Model skyBox = scene.skybox;
        skyBox.setPos(camera.getPos());
        Matrix modelViewMatrix = camera.getWorldToCam().matMul(skyBox.getObjectToWorldMatrix());
        skyBoxShaderProgram.setUniform("modelViewMatrix", modelViewMatrix);
        skyBoxShaderProgram.setUniform("ambientLight", skyBox.mesh.materials.get(0).ambientColor);

        scene.skybox.getMesh().initToEndFullRender(1);

        skyBoxShaderProgram.unbind();
    }

    public void renderDepthMap(long window, Camera camera, Scene scene, HUD hud) {

        glCullFace(GL_BACK);
        directionalLightDepthShaderProgram.bind();
        worldToDirectionalLights = new ArrayList<>();
        directionalLightDepthShaderProgram.setUniform("orthoProjectionMatrix", ortho);

        for(int i =0;i < scene.directionalLights.size();i++) {

            DirectionalLight light = scene.directionalLights.get(i);
            glViewport(0,0, light.shadowMap.shadowMapWidth, light.shadowMap.shadowMapHeight);
            glBindFramebuffer(GL_FRAMEBUFFER, light.shadowMap.depthMapFBO);
            glClear(GL_DEPTH_BUFFER_BIT);

            Matrix worldToLight = light.getWorldToObject();
            worldToDirectionalLights.add(worldToLight);

            for (Mesh mesh : scene.modelMap.keySet()) {
                mesh.initRender();
                for (Model m : scene.modelMap.get(mesh)) {
                    if (m.isOpaque) {
                        Matrix modelLightViewMatrix = worldToLight.matMul(m.getObjectToWorldMatrix());
                        directionalLightDepthShaderProgram.setUniform("modelLightViewMatrix", modelLightViewMatrix);
                        mesh.render();
                    }
                }
                mesh.endRender();
            }
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
        }

        directionalLightDepthShaderProgram.unbind();
        glCullFace(GL_BACK);
    }

    public void cleanUp() {
        if(sceneShaderProgram != null) {
            sceneShaderProgram.cleanUp();
        }
    }

}

