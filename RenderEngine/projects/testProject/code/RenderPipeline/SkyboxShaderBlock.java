package RenderPipeline;

import engine.Math.Matrix;
import engine.model.Model;
import engine.renderingEngine.RenderBlockInput;
import engine.shader.ShaderProgram;
import engine.utils.Utils;

import java.io.File;
import java.nio.file.Path;

public class SkyboxShaderBlock extends engine.renderingEngine.RenderBlock {

    public static String skyboxShaderID = "skyboxshader";
    ShaderProgram skyboxShader;

    public SkyboxShaderBlock(String id) {
        super(id);
    }

    @Override
    public void setup(RenderBlockInput input) {
        try {
            skyboxShader = new ShaderProgram(skyboxShaderID);

            Path thisFilePath = Utils.getClassPath("SkyboxShaderBlock.java");
            File shadersDir = thisFilePath.getParent().getParent().getParent().toFile();

            skyboxShader.createVertexShader(shadersDir.getAbsolutePath()+"/Shaders/SkyBoxVertexShader.glsl");
            skyboxShader.createFragmentShader(shadersDir.getAbsolutePath()+"/Shaders/SkyBoxFragmentShader.glsl");
            skyboxShader.link();

            skyboxShader.createUniform("projectionMatrix");
            skyboxShader.createUniform("modelViewMatrix");
            skyboxShader.createUniform("texture_sampler");
            skyboxShader.createUniform("ambientLight");
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    @Override
    public void render(RenderBlockInput input) {

        if(input.scene.skybox == null) {
            return;
        }

        ShaderProgram skyBoxShaderProgram = skyboxShader;
        skyBoxShaderProgram.bind();

        if (input.scene.skybox.shouldRender) {
            skyBoxShaderProgram.setUniform("texture_sampler", 0);

            // Update projection Matrix
            Matrix projectionMatrix = input.scene.camera.getPerspectiveProjectionMatrix();
            skyBoxShaderProgram.setUniform("projectionMatrix", projectionMatrix);

            Model skyBox = input.scene.skybox;
            skyBox.setPos(input.scene.camera.getPos());
            Matrix modelViewMatrix = input.scene.camera.getWorldToCam().matMul(input.scene.skybox.getObjectToWorldMatrix());
            skyBoxShaderProgram.setUniform("modelViewMatrix", modelViewMatrix);
            skyBoxShaderProgram.setUniform("ambientLight", skyBox.mesh.materials.get(0).ambientColor);

            input.scene.skybox.getMesh().initToEndFullRender(0);
        }
        skyBoxShaderProgram.unbind();

    }

    @Override
    public void cleanUp() {
        skyboxShader.cleanUp();
    }
}
