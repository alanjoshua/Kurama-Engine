package Kurama.renderingEngine.defaultRenderPipeline;

import Kurama.Math.Matrix;
import Kurama.game.Game;
import Kurama.ComponentSystem.components.model.Model;
import Kurama.renderingEngine.*;
import Kurama.shader.ShaderProgramGL;

public class SkyboxShaderBlock extends Kurama.renderingEngine.RenderPipeline {

    private static String skyboxShaderID = "skyboxshader";
    private ShaderProgramGL skyboxShader;

    public SkyboxShaderBlock(Game game, RenderPipeline parentPipeline, String pipelineID) {
        super(game, parentPipeline, pipelineID);
    }

    @Override
    public void setup(RenderPipelineData input) {
        try {
            skyboxShader = new ShaderProgramGL(skyboxShaderID);

            skyboxShader.createVertexShader("src/main/java/Kurama/renderingEngine/defaultRenderPipeline/shaders/SkyBoxVertexShader.glsl");
            skyboxShader.createFragmentShader("src/main/java/Kurama/renderingEngine/defaultRenderPipeline/shaders/SkyBoxFragmentShader.glsl");
            skyboxShader.link();

            skyboxShader.createUniform("projectionMatrix");
            skyboxShader.createUniform("modelViewMatrix");
            skyboxShader.createUniform("texture_sampler");
            skyboxShader.createUniform("ambientLight");

            skyboxShader.setUniform("texture_sampler", 0);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    @Override
    public RenderPipelineData render(RenderPipelineData input) {

        if(input.scene.skybox == null) {
            return null;
        }

        CurrentCameraBlockData inp = (CurrentCameraBlockData) input;
        var camera = inp.camera;

        ShaderProgramGL skyBoxShaderProgramGL = skyboxShader;
        skyBoxShaderProgramGL.bind();

        if (input.scene.skybox.shouldRender) {

            // Update projection Matrix
            Matrix projectionMatrix = camera.getPerspectiveProjectionMatrix();
            skyBoxShaderProgramGL.setUniform("projectionMatrix", projectionMatrix);

            Model skyBox = input.scene.skybox;
            skyBox.setPos(camera.getPos());
            Matrix modelViewMatrix = camera.getWorldToObject().matMul(input.scene.skybox.getObjectToWorldMatrix());
            skyBoxShaderProgramGL.setUniform("modelViewMatrix", modelViewMatrix);
            skyBoxShaderProgramGL.setUniform("ambientLight", skyBox.meshes.get(0).materials.get(0).ambientColor);

            ((DefaultRenderPipeline)parentPipeline).initToEndFullRender(input.scene.skybox.meshes.get(0), 0);
        }
        skyBoxShaderProgramGL.unbind();
        return input;
    }

    @Override
    public void cleanUp() {
        skyboxShader.cleanUp();
    }
}
