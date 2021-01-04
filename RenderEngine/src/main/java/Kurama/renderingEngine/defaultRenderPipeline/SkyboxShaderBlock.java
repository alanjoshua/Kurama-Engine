package Kurama.renderingEngine.defaultRenderPipeline;

import Kurama.Math.Matrix;
import Kurama.model.Model;
import Kurama.renderingEngine.CurrentCameraBlockInput;
import Kurama.renderingEngine.RenderBlockInput;
import Kurama.renderingEngine.RenderBlockOutput;
import Kurama.renderingEngine.RenderPipeline;
import Kurama.shader.ShaderProgram;

public class SkyboxShaderBlock extends Kurama.renderingEngine.RenderBlock {

    private static String skyboxShaderID = "skyboxshader";
    private ShaderProgram skyboxShader;

    public SkyboxShaderBlock(String id, RenderPipeline pipeline) {
        super(id, pipeline);
    }

    @Override
    public void setup(RenderBlockInput input) {
        try {
            skyboxShader = new ShaderProgram(skyboxShaderID);

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
    public RenderBlockOutput render(RenderBlockInput input) {

        if(input.scene.skybox == null) {
            return null;
        }

        CurrentCameraBlockInput inp = (CurrentCameraBlockInput) input;
        var camera = inp.camera;

        ShaderProgram skyBoxShaderProgram = skyboxShader;
        skyBoxShaderProgram.bind();

        if (input.scene.skybox.shouldRender) {

            // Update projection Matrix
            Matrix projectionMatrix = camera.getPerspectiveProjectionMatrix();
            skyBoxShaderProgram.setUniform("projectionMatrix", projectionMatrix);

            Model skyBox = input.scene.skybox;
            skyBox.setPos(camera.getPos());
            Matrix modelViewMatrix = camera.getWorldToCam().matMul(input.scene.skybox.getObjectToWorldMatrix());
            skyBoxShaderProgram.setUniform("modelViewMatrix", modelViewMatrix);
            skyBoxShaderProgram.setUniform("ambientLight", skyBox.meshes.get(0).materials.get(0).ambientColor);

            ((DefaultRenderPipeline)renderPipeline).initToEndFullRender(input.scene.skybox.meshes.get(0), 0);
        }
        skyBoxShaderProgram.unbind();
        return null;
    }

    @Override
    public void cleanUp() {
        skyboxShader.cleanUp();
    }
}
