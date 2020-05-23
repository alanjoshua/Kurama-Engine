package engine.renderingEngine;

import engine.DataStructure.Mesh.Mesh;
import engine.DataStructure.Scene;
import engine.Effects.ShadowMap;
import engine.HUD;
import engine.Math.Matrix;
import engine.Math.Quaternion;
import engine.Math.Vector;
import engine.Effects.Material;
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
    public ShadowMap shadowMap;
    protected Mesh axes;
    public Matrix ortho;
    public Matrix worldToLight;

    public void init() {
        axes = MeshBuilder.buildAxes();
        axes.material = new Material(new Vector(new float[]{1,1,1,1}),1);

        glEnable(GL_DEPTH_TEST);    //Enables depth testing

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        setupShadowMaps();
        setupSceneShader(game.scene);
        setupDirectionalLightDepthShader();
        setupHUDShader();
        setupSkybox();

    }

    public void setupShadowMaps() {
        shadowMap = new ShadowMap(ShadowMap.DEFAULT_SHADOWMAP_WIDTH*2,ShadowMap.DEFAULT_SHADOWMAP_HEIGHT*2);
        ortho = Matrix.buildOrthographicProjectionMatrix(1,-100,50,-50,-50,50);
    }

    public void setupSceneShader(Scene scene) {
        try {
            sceneShaderProgram = new ShaderProgram();
            sceneShaderProgram.createVertexShader(Utils.loadResourceAsString("/Shaders/SceneVertexShader.vs"));
            sceneShaderProgram.createFragmentShader(Utils.loadResourceAsString("/Shaders/SceneFragmentShader.fs"));
            sceneShaderProgram.link();

            sceneShaderProgram.createUniform("projectionMatrix");
            sceneShaderProgram.createUniform("orthoProjectionMatrix");
            sceneShaderProgram.createUniform("modelLightViewMatrix");
            sceneShaderProgram.createUniform("shadowMap");
            sceneShaderProgram.createUniform("modelViewMatrix");
            sceneShaderProgram.createUniform("texture_sampler");
            sceneShaderProgram.createUniform("normalMap");
            sceneShaderProgram.createUniform("diffuseMap");
            sceneShaderProgram.createUniform("specularMap");

            sceneShaderProgram.createMaterialUniform("material");

            sceneShaderProgram.createUniform("specularPower");
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
       scene.models.get(1).mesh.material.texture = shadowMap.depthMap;
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

        sceneShaderProgram.setUniform("texture_sampler",0);
        sceneShaderProgram.setUniform("normalMap",1);
        sceneShaderProgram.setUniform("diffuseMap",2);
        sceneShaderProgram.setUniform("specularMap",3);
        sceneShaderProgram.setUniform("shadowMap",4);
        sceneShaderProgram.setUniform("projectionMatrix",projectionMatrix);
        sceneShaderProgram.setUniform("orthoProjectionMatrix",ortho);

        sceneShaderProgram.setUniform("ambientLight",scene.ambientLight);
        sceneShaderProgram.setUniform("specularPower",scene.specularPower);

        LightDataPackage lights = processLights(scene.pointLights, scene.spotLights, scene.directionalLights, worldToCam);
        sceneShaderProgram.setUniform("spotLights",lights.spotLights);
        sceneShaderProgram.setUniform("pointLights",lights.pointLights);
        sceneShaderProgram.setUniform("directionalLights",lights.directionalLights);

        sceneShaderProgram.setUniform("fog", scene.fog);
        glBindTexture(GL_TEXTURE_2D, shadowMap.depthMap.getId());

        Vector c = new Vector(3,0);
        for(DirectionalLight l:scene.directionalLights) {
            c = c.add(l.color.scalarMul(l.intensity));
        }
        sceneShaderProgram.setUniform("allDirectionalLightStatic", c);

        Map<Mesh, List<Model>> accessoryModels = new HashMap<>();
        int indvRenderCalls = 0;

        for(Mesh mesh:scene.modelMap.keySet()) {
            sceneShaderProgram.setUniform("material", mesh.material);
            mesh.initRender();
            glActiveTexture(GL_TEXTURE4);
            glBindTexture(GL_TEXTURE_2D,shadowMap.depthMap.getId());
            indvRenderCalls++;
            for(Model model:scene.modelMap.get(mesh)) {
                Matrix objectToWorld =  model.getObjectToWorldMatrix();
                sceneShaderProgram.setUniform("modelViewMatrix",worldToCam.matMul(objectToWorld));
                sceneShaderProgram.setUniform("modelLightViewMatrix", worldToLight.matMul(objectToWorld));
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

        }
        for(Mesh mesh:accessoryModels.keySet()) {
            sceneShaderProgram.setUniform("material", mesh.material);
            indvRenderCalls++;
            mesh.initRender();
            for(Model model:accessoryModels.get(mesh)) {
                Matrix objectToWorld =  model.getObjectToWorldMatrix();
                sceneShaderProgram.setUniform("modelViewMatrix",worldToCam.matMul(objectToWorld));
                sceneShaderProgram.setUniform("modelLightViewMatrix", worldToLight.matMul(objectToWorld));
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
        for(Model m: hud.hudElements) {
            Matrix ortho = Matrix.buildOrtho2D(0, game.getDisplay().getWidth(), game.getDisplay().getHeight(), 0);

            // Set orthographic and model matrix for this HUD item
            Matrix projModelMatrix = ortho.matMul((m.getObjectToWorldMatrix()));

            hudShaderProgram.setUniform("projModelMatrix", projModelMatrix);
            hudShaderProgram.setUniform("color", m.mesh.material.ambientColor);

            m.mesh.initToEndFullRender();
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
        skyBoxShaderProgram.setUniform("ambientLight", skyBox.mesh.material.ambientColor);

        scene.skybox.getMesh().initToEndFullRender();

        skyBoxShaderProgram.unbind();
    }

    public void renderDepthMap(long window, Camera camera, Scene scene, HUD hud) {

        glBindFramebuffer(GL_FRAMEBUFFER,shadowMap.depthMapFBO);
        glClear(GL_DEPTH_BUFFER_BIT);
        glViewport(0,0,shadowMap.shadowMapWidth,shadowMap.shadowMapHeight);
        directionalLightDepthShaderProgram.bind();

        DirectionalLight light = scene.directionalLights.get(0);
//        float angleX = (float)Math.toDegrees(Math.acos(rotMatrix.getColumn(2).get(2)));
//        float angleY = (float)Math.toDegrees(Math.asin(rotMatrix.getColumn(2).get(0)));
//        float angleZ = 0;

//        float scale = 50;

//        light.direction.scalarMul(scale).display();
//        Quaternion qt = Quaternion.getQuaternionFromEuler(angleX,angleY,angleZ);
//        Vector pos = new Vector(new float[]{0,10,0});
//        Matrix m_ = qt.getRotationMatrix();
//        Vector pos_ = (m_.matMul(light.direction.scalarMul(scale)).toVector().scalarMul(-1));
//        lightViewMatrix = m_.addColumn(pos_);
//        lightViewMatrix = lightViewMatrix.addRow(new Vector(new float[]{0,0,0,1}));

        Model temp = new Model(light.game,light.mesh,light.identifier);
        temp.setOrientation(light.getOrientation().getInverse());
        temp.setPos(light.getPos());

        worldToLight = light.getWorldToObject();

        directionalLightDepthShaderProgram.setUniform("orthoProjectionMatrix",ortho);
        for(Mesh mesh: scene.modelMap.keySet()) {
            mesh.initRender();
            for(Model m:scene.modelMap.get(mesh)) {
                if(m.isOpaque) {
                    Matrix modelLightViewMatrix = worldToLight.matMul(m.getObjectToWorldMatrix());
                    directionalLightDepthShaderProgram.setUniform("modelLightViewMatrix", modelLightViewMatrix);
                    mesh.render();
                }
            }
            mesh.endRender();
        }

        directionalLightDepthShaderProgram.unbind();
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void cleanUp() {
        if(sceneShaderProgram != null) {
            sceneShaderProgram.cleanUp();
        }
    }

}

