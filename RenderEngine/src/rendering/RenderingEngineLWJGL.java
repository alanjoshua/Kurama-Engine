package rendering;

import Math.Matrix;
import Math.Utils;
import Shaders.ShaderProgram;
import main.GameLWJGL;
import models.ModelLWJGL;

import java.util.List;

import static org.lwjgl.opengl.GL11.*;

public class RenderingEngineLWJGL {

    GameLWJGL game;
    public ShaderProgram shaderProgram;

    public enum ProjectionMode {
        ORTHO, PERSPECTIVE
    }

    public enum RenderPipeline {
        Matrix, Quat
    }

    public void init() throws Exception {
        glEnable(GL_DEPTH_TEST);    //Enables depth testing
        game.getDisplay().setClearColor(0,0,0,1);

        shaderProgram = new ShaderProgram();
        shaderProgram.createVertexShader(Utils.loadResourceAsString("/Shaders/VertexShader.vs"));
        shaderProgram.createFragmentShader(Utils.loadResourceAsString("/Shaders/FragmentShader.fs"));
        shaderProgram.link();

        shaderProgram.createUniform("projectionMatrix");
        shaderProgram.createUniform("worldMatrix");
       // shaderProgram.createUniform("texture_sampler");

        enableModelFill();

    }

    private ProjectionMode projectionMode = ProjectionMode.PERSPECTIVE;
    private RenderPipeline renderPipeline = RenderPipeline.Quat;

    public RenderingEngineLWJGL(GameLWJGL game) {
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

    public void render3(List<ModelLWJGL> models) {
        clear();
        shaderProgram.bind();

        Matrix worldToCam = game.getCamera().getWorldToCam();
        Matrix projectionMatrix = game.getCamera().getPerspectiveProjectionMatrix();


        shaderProgram.setUniform("projectionMatrix",projectionMatrix);

        for(ModelLWJGL model: models) {
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

    public ProjectionMode getProjectionMode() {
        return projectionMode;
    }

    public void setProjectionMode(ProjectionMode projectionMode) {
        this.projectionMode = projectionMode;
    }

    public RenderPipeline getRenderPipeline() {
        return renderPipeline;
    }

    public void setRenderPipeline(RenderPipeline renderPipeline) {
        this.renderPipeline = renderPipeline;
    }

}
