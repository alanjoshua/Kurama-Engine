package main;

import engine.Math.Matrix;
import engine.Math.Vector;
import engine.lighting.PointLight;
import engine.renderingEngine.RenderingEngine;
import engine.utils.Utils;
import engine.shader.ShaderProgram;
import engine.game.Game;
import engine.model.Model;

import java.util.List;

import static org.lwjgl.opengl.GL11.*;

public class RenderingEngineLWJGL extends RenderingEngine {

    public ShaderProgram shaderProgram;
    public GameLWJGL game;

    public void init() {
        glEnable(GL_DEPTH_TEST);    //Enables depth testing
        
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

//        glPolygonMode(GL_FRONT_AND_BACK,GL_LINE);

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
            shaderProgram.createPointLightUniform("pointLight");

        } catch (Exception e) {
            e.printStackTrace();
        }

        //enableModelFill();

    }

    public RenderingEngineLWJGL(GameLWJGL game) {
        super(game);
        this.game = game;
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

        PointLight currLight = new PointLight(game.pointLight);
        currLight.pos = worldToCam.matMul(currLight.pos.addDimensionToVec(1)).getColumn(0);
        shaderProgram.setUniform("pointLight",currLight);

        for(Model model: models) {
            shaderProgram.setUniform("modelViewMatrix",worldToCam.matMul(model.getObjectToWorldMatrix()));
            shaderProgram.setUniform("material", model.mesh.material);
            model.mesh.render();
        }

        shaderProgram.unbind();

    }

    public void cleanUp() {
        if(shaderProgram != null) {
            shaderProgram.cleanUp();
        }
    }

}
