package engine.renderingEngine.defaultRenderPipeline;

import engine.Math.Matrix;
import engine.model.Model;
import engine.renderingEngine.RenderBlockInput;
import engine.shader.ShaderProgram;

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

            skyboxShader.createVertexShader("src/engine/renderingEngine/defaultShaders/SkyBoxVertexShader.glsl");
            skyboxShader.createFragmentShader("src/engine/renderingEngine/defaultShaders/SkyBoxFragmentShader.glsl");
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
            skyBoxShaderProgram.setUniform("ambientLight", skyBox.meshes.get(0).materials.get(0).ambientColor);

            input.scene.skybox.meshes.get(0).initToEndFullRender(0);
        }
        skyBoxShaderProgram.unbind();

    }

    @Override
    public void cleanUp() {
        skyboxShader.cleanUp();
    }
}
