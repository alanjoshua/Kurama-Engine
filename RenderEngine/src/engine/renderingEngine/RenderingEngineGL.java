package engine.renderingEngine;

import engine.DataStructure.Mesh.Mesh;
import engine.DataStructure.Scene;
import engine.HUD;
import engine.Math.Matrix;
import engine.Math.Vector;
import engine.Effects.Material;
import engine.lighting.DirectionalLight;
import engine.model.ModelBuilder;
import engine.utils.Utils;
import engine.shader.ShaderProgram;
import engine.game.Game;
import engine.model.Model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

public class RenderingEngineGL extends RenderingEngine {

    public ShaderProgram sceneShaderProgram;
    public ShaderProgram hudShaderProgram;
    public ShaderProgram skyBoxShaderProgram;
    protected Mesh axes;

    public void init() {

        axes = ModelBuilder.buildAxes();
        axes.material = new Material(new Vector(new float[]{1,1,1,1}),1);

        glEnable(GL_DEPTH_TEST);    //Enables depth testing

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        setupSceneShader(game.scene);
        setupHUDShader();
        setupSkybox();

    }

    public void setupSceneShader(Scene scene) {
        try {
            sceneShaderProgram = new ShaderProgram();
            sceneShaderProgram.createVertexShader(Utils.loadResourceAsString("/Shaders/VertexShader.vs"));
            sceneShaderProgram.createFragmentShader(Utils.loadResourceAsString("/Shaders/FragmentShader.fs"));
            sceneShaderProgram.link();

            sceneShaderProgram.createUniform("projectionMatrix");
            sceneShaderProgram.createUniform("modelViewMatrix");
            sceneShaderProgram.createUniform("texture_sampler");
            sceneShaderProgram.createUniform("normalMap");

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

    public void render(Scene scene, HUD hud) {
        clear();
        renderScene(scene);
        renderSkyBox(scene);
        renderHUD(hud);
    }

    public void renderScene(Scene scene) {
        sceneShaderProgram.bind();

        Matrix worldToCam = game.getCamera().getWorldToCam();
        Matrix projectionMatrix = game.getCamera().getPerspectiveProjectionMatrix();

        sceneShaderProgram.setUniform("texture_sampler",0);
        sceneShaderProgram.setUniform("normalMap",1);
        sceneShaderProgram.setUniform("projectionMatrix",projectionMatrix);

        sceneShaderProgram.setUniform("ambientLight",scene.ambientLight);
        sceneShaderProgram.setUniform("specularPower",scene.specularPower);

        LightDataPackage lights = processLights(scene.pointLights, scene.spotLights, scene.directionalLights, worldToCam);
        sceneShaderProgram.setUniform("spotLights",lights.spotLights);
        sceneShaderProgram.setUniform("pointLights",lights.pointLights);
        sceneShaderProgram.setUniform("directionalLights",lights.directionalLights);

        sceneShaderProgram.setUniform("fog", scene.fog);
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
            indvRenderCalls++;
            for(Model model:scene.modelMap.get(mesh)) {
                sceneShaderProgram.setUniform("modelViewMatrix",worldToCam.matMul(model.getObjectToWorldMatrix()));
                mesh.justRender();

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
                sceneShaderProgram.setUniform("modelViewMatrix",worldToCam.matMul(model.getObjectToWorldMatrix()));
                mesh.justRender();
            }
            mesh.endRender();
        }

//        System.out.println("indv render valls: "+indvRenderCalls);

//        for(Model model: scene.models) {
//            sceneShaderProgram.setUniform("material", model.mesh.material);
//            sceneShaderProgram.setUniform("modelViewMatrix",worldToCam.matMul(model.getObjectToWorldMatrix()));
//           // sceneShaderProgram.setUniform("material.hasTexture", model.shouldDisplayTexture?1:0);
//            model.mesh.initRender();
//            model.mesh.justRender();
//            model.mesh.endRender();
//            sceneShaderProgram.setUniform("material.hasTexture", 0);
//            if(model.shouldShowCollisionBox && model.boundingbox != null) {
//                sceneShaderProgram.setUniform("material", model.boundingbox.material);
//                model.boundingbox.render();
//            }
//            if(model.shouldShowAxes && axes != null) {
//                sceneShaderProgram.setUniform("material", axes.material);
//                axes.render();
//            }
//        }

        sceneShaderProgram.unbind();
    }

    public void renderHUD(HUD hud) {

        if(hud == null) {
            return;
        }

        for(Model m: hud.hudElements) {
            hudShaderProgram.bind();

            Matrix ortho = buildOrtho2D(0, game.getDisplay().getWidth(), game.getDisplay().getHeight(), 0);

            // Set orthographic and model matrix for this HUD item
            Matrix projModelMatrix = ortho.matMul((m.getObjectToWorldMatrix()));

            hudShaderProgram.setUniform("projModelMatrix", projModelMatrix);
            hudShaderProgram.setUniform("color", m.mesh.material.ambientColor);

            m.mesh.render();
        }

        hudShaderProgram.unbind();

    }

    public void renderSkyBox(Scene scene) {

        if(scene.skybox == null) {
            return;
        }

        skyBoxShaderProgram.bind();

        skyBoxShaderProgram.setUniform("texture_sampler", 0);

        // Update projection Matrix
        Matrix projectionMatrix = game.getCamera().getPerspectiveProjectionMatrix();
        skyBoxShaderProgram.setUniform("projectionMatrix", projectionMatrix);

        Model skyBox = scene.skybox;
        skyBox.setPos(game.getCamera().getPos());
        Matrix modelViewMatrix = game.getCamera().getWorldToCam().matMul(skyBox.getObjectToWorldMatrix());
        skyBoxShaderProgram.setUniform("modelViewMatrix", modelViewMatrix);
        skyBoxShaderProgram.setUniform("ambientLight", skyBox.mesh.material.ambientColor);

        scene.skybox.getMesh().render();

        skyBoxShaderProgram.unbind();
    }

    public void cleanUp() {
        if(sceneShaderProgram != null) {
            sceneShaderProgram.cleanUp();
        }
    }

}

