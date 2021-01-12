package Kurama.renderingEngine.defaultRenderPipeline;

import Kurama.Math.Matrix;
import Kurama.Mesh.Mesh;
import Kurama.model.Model;
import Kurama.renderingEngine.RenderBlockInput;
import Kurama.renderingEngine.RenderBlockOutput;
import Kurama.renderingEngine.RenderPipeline;
import Kurama.shader.ShaderProgram;

public class HUD_ShaderBlock extends Kurama.renderingEngine.RenderBlock {

    private String hud_shader_id = "hud_shader";
    private ShaderProgram hud_shader;

    public HUD_ShaderBlock(String id, RenderPipeline pipeline) {
        super(id, pipeline);
    }

    @Override
    public void setup(RenderBlockInput input) {

        try {

            hud_shader = new ShaderProgram(hud_shader_id);

            hud_shader.createVertexShader("src/main/java/Kurama/renderingEngine/defaultRenderPipeline/shaders/HUDVertexShader.glsl");
            hud_shader.createFragmentShader("src/main/java/Kurama/renderingEngine/defaultRenderPipeline/shaders/HUDFragmentShader.glsl");
            hud_shader.link();

            // Create uniforms for Orthographic-model projection matrix and base colour
            hud_shader.createUniform("texture_sampler");
            hud_shader.createUniform("projModelMatrix");
            hud_shader.createUniform("color");

            hud_shader.setUniform("texture_sampler", 0);

        }catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    @Override
    public RenderBlockOutput render(RenderBlockInput input) {

        Matrix ortho = Matrix.buildOrtho2D(0, input.game.getMasterWindow().width, input.game.getMasterWindow().height, 0);

        ShaderProgram hudShaderProgram = hud_shader;
        hudShaderProgram.bind();

        for(String meshId :input.scene.shaderblock_mesh_model_map.get(pipelineID).keySet()) {
            Mesh mesh = input.scene.meshID_mesh_map.get(meshId);

            for(String modelId : input.scene.shaderblock_mesh_model_map.get(pipelineID).get(meshId).keySet()) {
                Model m = input.scene.modelID_model_map.get(modelId);

                if (m.shouldRender) {
                    // Set orthographic and model matrix for this HUD item
                    Matrix projModelMatrix = ortho.matMul((m.getObjectToWorldMatrix()));
                    hudShaderProgram.setUniform("projModelMatrix", projModelMatrix);
                    hudShaderProgram.setUniform("color", mesh.materials.get(0).ambientColor);

                    ((DefaultRenderPipeline)renderPipeline).initToEndFullRender(mesh, 0);
                }
            }
        }

        hudShaderProgram.unbind();
        return null;
    }

    @Override
    public void cleanUp() {
        hud_shader.cleanUp();
    }
}
