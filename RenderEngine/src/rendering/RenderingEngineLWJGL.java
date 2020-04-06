package rendering;

import Math.Matrix;
import Math.Utils;
import Shaders.ShaderProgram;
import main.Game;
import models.Model;

import java.util.List;

import static org.lwjgl.opengl.GL11.*;

public class RenderingEngineLWJGL extends RenderingEngine {

    public ShaderProgram shaderProgram;

    public void init() {
        glEnable(GL_DEPTH_TEST);    //Enables depth testing

        try {
            shaderProgram = new ShaderProgram();
            shaderProgram.createVertexShader(Utils.loadResourceAsString("/Shaders/VertexShader.vs"));
            shaderProgram.createFragmentShader(Utils.loadResourceAsString("/Shaders/FragmentShader.fs"));
            shaderProgram.link();

            shaderProgram.createUniform("projectionMatrix");
            shaderProgram.createUniform("worldMatrix");
        } catch (Exception e) {
            e.printStackTrace();
        }

        //enableModelFill();

    }

    public RenderingEngineLWJGL(Game game) {
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


        shaderProgram.setUniform("projectionMatrix",projectionMatrix);

        for(Model model: models) {
            shaderProgram.setUniform("worldMatrix",worldToCam.matMul(model.getObjectToWorldMatrix()));
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
