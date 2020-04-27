package engine.renderingEngine;

import engine.DataStructure.Mesh.Mesh;
import engine.Math.Matrix;
import engine.Math.Vector;
import engine.lighting.DirectionalLight;
import engine.lighting.Material;
import engine.lighting.PointLight;
import engine.lighting.SpotLight;
import engine.model.ModelBuilder;
import engine.renderingEngine.RenderingEngine;
import engine.utils.Utils;
import engine.shader.ShaderProgram;
import engine.game.Game;
import engine.model.Model;
import main.GameLWJGL;
import org.lwjgl.system.CallbackI;

import java.util.List;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL11.*;

public class RenderingEngineGL extends RenderingEngine {

    public ShaderProgram shaderProgram;
    protected Mesh axes;

    public void init() {

        axes = ModelBuilder.buildAxes();
        axes.material = new Material(new Vector(new float[]{1,1,1,1}),1);

        glEnable(GL_DEPTH_TEST);    //Enables depth testing

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        try {
            shaderProgram = new ShaderProgram();
            shaderProgram.createVertexShader(Utils.loadResourceAsString("/Shaders/VertexShader.vs"));
            shaderProgram.createFragmentShader(Utils.loadResourceAsString("/Shaders/FragmentShader.fs"));
            shaderProgram.link();

            shaderProgram.createUniform("projectionMatrix");
            shaderProgram.createUniform("modelViewMatrix");
            shaderProgram.createUniform("texture_sampler");

            shaderProgram.createMaterialUniform("material");

            shaderProgram.createUniform("specularPower");
            shaderProgram.createUniform("ambientLight");

            shaderProgram.createPointLightListUniform("pointLights",game.pointLights.size());
            shaderProgram.createDirectionalLightListUniform("directionalLights",game.directionalLights.size());
            shaderProgram.createSpotLightListUniform("spotLights",game.spotLights.size());

        } catch (Exception e) {
            e.printStackTrace();
        }

        //enableModelFill();

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
        shaderProgram.bind();

        Matrix worldToCam = game.getCamera().getWorldToCam();
        Matrix projectionMatrix = game.getCamera().getPerspectiveProjectionMatrix();

        shaderProgram.setUniform("texture_sampler",0);
        shaderProgram.setUniform("projectionMatrix",projectionMatrix);

        shaderProgram.setUniform("ambientLight",game.ambientLight);
        shaderProgram.setUniform("specularPower",game.specularPower);

        LightDataPackage lights = processLights(game.pointLights, game.spotLights, game.directionalLights, worldToCam);
        shaderProgram.setUniform("spotLights",lights.spotLights);
        shaderProgram.setUniform("pointLights",lights.pointLights);
        shaderProgram.setUniform("directionalLights",lights.directionalLights);


        for(Model model: models) {
            shaderProgram.setUniform("modelViewMatrix",worldToCam.matMul(model.getObjectToWorldMatrix()));
            shaderProgram.setUniform("material", model.mesh.material);

            model.mesh.render();

            shaderProgram.setUniform("material.hasTexture", 0);

            if(model.shouldShowCollisionBox && model.boundingbox != null) {
                shaderProgram.setUniform("material", model.boundingbox.material);
                model.boundingbox.render();
            }

            if(model.shouldShowAxes && axes != null) {
                shaderProgram.setUniform("material", axes.material);
                axes.render();
            }

        }

        shaderProgram.unbind();

    }

    public void cleanUp() {
        if(shaderProgram != null) {
            shaderProgram.cleanUp();
        }
    }

}

