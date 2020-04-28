package engine.renderingEngine;

import engine.DataStructure.Mesh.Mesh;
import engine.GUI.Text;
import engine.Math.Matrix;
import engine.Math.Vector;
import engine.lighting.Material;
import engine.model.ModelBuilder;
import engine.utils.Utils;
import engine.shader.ShaderProgram;
import engine.game.Game;
import engine.model.Model;
import org.lwjgl.system.CallbackI;

import java.util.List;

import static org.lwjgl.opengl.GL11.*;

public class RenderingEngineGL extends RenderingEngine {

    public ShaderProgram sceneShaderProgram;
    public ShaderProgram hudShaderProgram;
    protected Mesh axes;
    public Text text;

    public void init() {

        axes = ModelBuilder.buildAxes();
        axes.material = new Material(new Vector(new float[]{1,1,1,1}),1);
        try {
            text = new Text(game, "Hello World", "textures/fontTexture.png", 16, 16, "text");

        }catch (Exception e) {
            e.printStackTrace();
        }

        glEnable(GL_DEPTH_TEST);    //Enables depth testing

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        setupSceneShader();
        setupHUDShader();

    }

    public void setupSceneShader() {
        try {
            sceneShaderProgram = new ShaderProgram();
            sceneShaderProgram.createVertexShader(Utils.loadResourceAsString("/Shaders/VertexShader.vs"));
            sceneShaderProgram.createFragmentShader(Utils.loadResourceAsString("/Shaders/FragmentShader.fs"));
            sceneShaderProgram.link();

            sceneShaderProgram.createUniform("projectionMatrix");
            sceneShaderProgram.createUniform("modelViewMatrix");
            sceneShaderProgram.createUniform("texture_sampler");

            sceneShaderProgram.createMaterialUniform("material");

            sceneShaderProgram.createUniform("specularPower");
            sceneShaderProgram.createUniform("ambientLight");

            sceneShaderProgram.createPointLightListUniform("pointLights",game.pointLights.size());
            sceneShaderProgram.createDirectionalLightListUniform("directionalLights",game.directionalLights.size());
            sceneShaderProgram.createSpotLightListUniform("spotLights",game.spotLights.size());

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

    public void render(List<Model> models) {
        clear();
        renderScene(models);
        renderHUD();
    }

    public void renderScene(List<Model> models) {
        sceneShaderProgram.bind();

        Matrix worldToCam = game.getCamera().getWorldToCam();
        Matrix projectionMatrix = game.getCamera().getPerspectiveProjectionMatrix();

        sceneShaderProgram.setUniform("texture_sampler",0);
        sceneShaderProgram.setUniform("projectionMatrix",projectionMatrix);

        sceneShaderProgram.setUniform("ambientLight",game.ambientLight);
        sceneShaderProgram.setUniform("specularPower",game.specularPower);

        LightDataPackage lights = processLights(game.pointLights, game.spotLights, game.directionalLights, worldToCam);
        sceneShaderProgram.setUniform("spotLights",lights.spotLights);
        sceneShaderProgram.setUniform("pointLights",lights.pointLights);
        sceneShaderProgram.setUniform("directionalLights",lights.directionalLights);


        for(Model model: models) {
            sceneShaderProgram.setUniform("modelViewMatrix",worldToCam.matMul(model.getObjectToWorldMatrix()));
            sceneShaderProgram.setUniform("material", model.mesh.material);

            model.mesh.render();

            sceneShaderProgram.setUniform("material.hasTexture", 0);

            if(model.shouldShowCollisionBox && model.boundingbox != null) {
                sceneShaderProgram.setUniform("material", model.boundingbox.material);
                model.boundingbox.render();
            }

            if(model.shouldShowAxes && axes != null) {
                sceneShaderProgram.setUniform("material", axes.material);
                axes.render();
            }

        }

        sceneShaderProgram.unbind();
    }

    public void renderHUD() {

        text.setPos(10, game.getDisplay().getHeight() - 100f, 0);

        hudShaderProgram.bind();

        Matrix ortho = buildOrtho2D(0, game.getDisplay().getWidth(), game.getDisplay().getHeight(),0);

        // Set orthographic and model matrix for this HUD item
        Matrix projModelMatrix = ortho.matMul((text.getObjectToWorldMatrix()));

        hudShaderProgram.setUniform("projModelMatrix", projModelMatrix);
        hudShaderProgram.setUniform("color",text.mesh.material.ambientColor);

        // Render the mesh for this HUD item
        text.mesh.render();

        hudShaderProgram.unbind();

    }

    public void cleanUp() {
        if(sceneShaderProgram != null) {
            sceneShaderProgram.cleanUp();
        }
    }

}

