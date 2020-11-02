package engine.renderingEngine;

import engine.DataStructure.Mesh.Mesh;
import engine.DataStructure.Scene;
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

    public ShaderProgram sceneShaderProgram;
    public ShaderProgram hudShaderProgram;
    public ShaderProgram skyBoxShaderProgram;
    public ShaderProgram directionalLightDepthShaderProgram;
    protected Mesh axes;
    public Matrix ortho = Matrix.buildOrthographicProjectionMatrix(1,-700,100,-100,-100,100);
    //public Matrix ortho = Matrix.buildOrthographicProjectionMatrix(1,700,-100,100,-100,100);

    public void init() {
        axes = MeshBuilder.buildAxes();

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

    public void setupSceneShader(Scene scene) {
        try {
            sceneShaderProgram = new ShaderProgram();
            sceneShaderProgram.createVertexShader(Utils.loadResourceAsString("/Shaders/SceneVertexShader.glsl"));
            sceneShaderProgram.createFragmentShader(Utils.loadResourceAsString("/Shaders/SceneFragmentShader.glsl"));
            sceneShaderProgram.link();

            sceneShaderProgram.createUniform("projectionMatrix");
            sceneShaderProgram.createUniform("orthoProjectionMatrix");
            sceneShaderProgram.createUniformArray("modelLightViewMatrix",5);
            sceneShaderProgram.createUniformArray("modelSpotLightViewMatrix",5);
            sceneShaderProgram.createUniformArray("directionalShadowMaps",5);
            sceneShaderProgram.createUniformArray("spotLightShadowMaps",5);
            sceneShaderProgram.createUniform("modelViewMatrix");
            sceneShaderProgram.createUniform("numDirectionalLights");
            sceneShaderProgram.createUniform("numberOfSpotLights");
            sceneShaderProgram.createMaterialListUniform("materials","mat_textures","mat_normalMaps","mat_diffuseMaps","mat_specularMaps",26);

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
        directionalLightDepthShaderProgram.createVertexShader(Utils.loadResourceAsString("/Shaders/depthDirectionalLightVertexShader.glsl"));
        directionalLightDepthShaderProgram.createFragmentShader(Utils.loadResourceAsString("/Shaders/depthDirectionalLightFragmentShader.glsl"));
        directionalLightDepthShaderProgram.link();

        directionalLightDepthShaderProgram.createUniform("orthoProjectionMatrix");
        directionalLightDepthShaderProgram.createUniform("modelLightViewMatrix");
//        directionalLightDepthShaderProgram.createUniform("nearZ");
//        directionalLightDepthShaderProgram.createUniform("farZ");
//        directionalLightDepthShaderProgram.createUniform("shouldLinearizeDepth");
    }

    public void setupHUDShader() {
        try {
        hudShaderProgram = new ShaderProgram();
        hudShaderProgram.createVertexShader(Utils.loadResourceAsString("/Shaders/HUDVertexShader.glsl"));
        hudShaderProgram.createFragmentShader(Utils.loadResourceAsString("/Shaders/HUDFragmentShader.glsl"));
        hudShaderProgram.link();

        // Create uniforms for Orthographic-model projection matrix and base colour
        hudShaderProgram.createUniform("texture_sampler");
        hudShaderProgram.createUniform("projModelMatrix");
        hudShaderProgram.createUniform("color");
        hudShaderProgram.createUniform("shouldGreyScale");

        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void setupSkybox() {
        try {
            skyBoxShaderProgram = new ShaderProgram();
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

    public void render(Scene scene, HUD hud, Camera camera) {
        ShadowDepthRenderPackage shadowPackage =  renderDepthMap(scene);
        glViewport(0,0,game.getDisplay().getWidth(),game.getDisplay().getHeight());
        clear();
        renderScene(scene,camera,shadowPackage);
        renderSkyBox(scene,camera);
        renderHUD(hud);
    }

    public void renderScene(Scene scene, Camera camera, ShadowDepthRenderPackage shadowPackage) {
        sceneShaderProgram.bind();

        Matrix worldToCam = camera.getWorldToCam();
        Matrix projectionMatrix = camera.getPerspectiveProjectionMatrix();

        sceneShaderProgram.setUniform("projectionMatrix",projectionMatrix);
        sceneShaderProgram.setUniform("orthoProjectionMatrix",ortho);

        sceneShaderProgram.setUniform("ambientLight",scene.ambientLight);

        LightDataPackage lights = processLights(scene.pointLights, scene.spotLights, scene.directionalLights, worldToCam);
        sceneShaderProgram.setUniform("spotLights",lights.spotLights);
        sceneShaderProgram.setUniform("pointLights",lights.pointLights);
        sceneShaderProgram.setUniform("directionalLights",lights.directionalLights);
        sceneShaderProgram.setUniform("numDirectionalLights",scene.directionalLights.size());
        sceneShaderProgram.setUniform("numberOfSpotLights",scene.spotLights.size());

        sceneShaderProgram.setUniform("fog", scene.fog);

        Vector c = new Vector(3,0);
        for(DirectionalLight l:scene.directionalLights) {
            c = c.add(l.color.scalarMul(l.intensity));
        }
        sceneShaderProgram.setUniform("allDirectionalLightStatic", c);

        Map<Mesh, List<Model>> accessoryModels = new HashMap<>();
        //int offset = 0;
        int offset = sceneShaderProgram.setAndActivateDirectionalShadowMaps("directionalShadowMaps", scene.directionalLights,0);
        offset = sceneShaderProgram.setAndActivateSpotLightShadowMaps("spotLightShadowMaps", scene.spotLights, offset);

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
                    sceneShaderProgram.setUniform("modelLightViewMatrix["+i+"]", shadowPackage.worldToDirectionalLights.get(i).matMul(objectToWorld));
                }
                for(int i = 0;i < scene.spotLights.size();i++) {
                    sceneShaderProgram.setUniform("modelSpotLightViewMatrix["+i+"]", shadowPackage.worldToSpotLights.get(i).matMul(objectToWorld));
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
                    sceneShaderProgram.setUniform("modelLightViewMatrix["+i+"]", shadowPackage.worldToDirectionalLights.get(i).matMul(objectToWorld));
                }
                for(int i = 0;i < scene.spotLights.size();i++) {
                    sceneShaderProgram.setUniform("modelSpotLightViewMatrix["+i+"]", shadowPackage.worldToSpotLights.get(i).matMul(objectToWorld));
                }
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

        Matrix ortho = Matrix.buildOrtho2D(0, game.getDisplay().getWidth(), game.getDisplay().getHeight(), 0);
        hudShaderProgram.bind();
        hudShaderProgram.setUniform("texture_sampler", 0);

        for(Model m: hud.hudElements) {
            // Set orthographic and model matrix for this HUD item
            Matrix projModelMatrix = ortho.matMul((m.getObjectToWorldMatrix()));
            hudShaderProgram.setUniform("projModelMatrix", projModelMatrix);
            hudShaderProgram.setUniform("color", m.mesh.materials.get(0).ambientColor);
            if (m.shouldGreyScale) {
                hudShaderProgram.setUniform("shouldGreyScale", 1);
            }
            else {
                hudShaderProgram.setUniform("shouldGreyScale", 0);
            }

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

        scene.skybox.getMesh().initToEndFullRender(0);

        skyBoxShaderProgram.unbind();
    }

    public ShadowDepthRenderPackage renderDepthMap(Scene scene) {

        glCullFace(GL_BACK);
        directionalLightDepthShaderProgram.bind();
        List<Matrix> worldToDirectionalLights = new ArrayList<>();
        List<Matrix> worldToSpotLights = new ArrayList<>();

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

        for(int i =0;i < scene.spotLights.size();i++) {
            SpotLight light = scene.spotLights.get(i);
            float aspectRatio = (float)light.shadowMap.shadowMapWidth/(float)light.shadowMap.shadowMapHeight;
            //ortho = Matrix.buildOrthographicProjectionMatrix(1,-700,100,-100,-100,100);
            Matrix projMatrix = Matrix.buildPerspectiveMatrix(light.angle*2,aspectRatio,1f,100,1,1);
            directionalLightDepthShaderProgram.setUniform("orthoProjectionMatrix", projMatrix);
            glViewport(0,0, light.shadowMap.shadowMapWidth, light.shadowMap.shadowMapHeight);
            glBindFramebuffer(GL_FRAMEBUFFER, light.shadowMap.depthMapFBO);
            glClear(GL_DEPTH_BUFFER_BIT);

            Matrix worldToLight = light.getWorldToObject();
            worldToSpotLights.add(projMatrix.matMul(worldToLight));

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
        if(sceneShaderProgram != null) {
            sceneShaderProgram.cleanUp();
        }
    }

}

