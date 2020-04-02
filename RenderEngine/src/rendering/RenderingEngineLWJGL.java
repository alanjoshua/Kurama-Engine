package rendering;

import Math.Matrix;
import Math.Quaternion;
import Math.Utils;
import Math.Vector;
import Shaders.ShaderProgram;
import main.GameLWJGL;
import models.DataStructure.Mesh.Face;
import models.DataStructure.Mesh.Vertex;
import models.Model;
import models.ModelBuilder;
import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class RenderingEngineLWJGL {

    GameLWJGL game;
    private static float viewingTolerance = 1.5f;
    public float[][] depthBuffer;   //Should be removed
    public Color[][] frameBuffer;  //Should be removed
    public ShaderProgram shaderProgram;
    public int vaoId;
    public int vboId;

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

    public void render3(List<Model> models) {
        clear();
        shaderProgram.bind();

        Matrix worldToCam = game.getCamera().getWorldToCam();
        Matrix projectionMatrix = game.getCamera().getPerspectiveProjectionMatrix();


        shaderProgram.setUniforms("projectionMatrix",projectionMatrix);

        for(Model model: models) {
            shaderProgram.setUniforms("worldMatrix",worldToCam.matMul(model.getObjectToWorldMatrix()));
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
