package engine.renderingEngine.defaultRenderPipeline;

import engine.Math.Matrix;
import engine.Mesh.Mesh;
import engine.model.Model;
import engine.renderingEngine.RenderBlockInput;
import engine.renderingEngine.RenderPipeline;
import engine.shader.ShaderProgram;

public class HUD_ShaderBlock extends engine.renderingEngine.RenderBlock {

    String hud_shader_id = "hud_shader";
    public ShaderProgram hud_shader;

    public HUD_ShaderBlock(String id, RenderPipeline pipeline) {
        super(id, pipeline);
    }

    @Override
    public void setup(RenderBlockInput input) {

        try {

            hud_shader = new ShaderProgram(hud_shader_id);

            hud_shader.createVertexShader("src/main/java/engine/renderingEngine/defaultShaders/HUDVertexShader.glsl");
            hud_shader.createFragmentShader("src/main/java/engine/renderingEngine/defaultShaders/HUDFragmentShader.glsl");
            hud_shader.link();

            // Create uniforms for Orthographic-model projection matrix and base colour
            hud_shader.createUniform("texture_sampler");
            hud_shader.createUniform("projModelMatrix");
            hud_shader.createUniform("color");

        }catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    @Override
    public void render(RenderBlockInput input) {
        if(input.scene.hud == null) {
            return;
        }

        Matrix ortho = Matrix.buildOrtho2D(0, input.game.getDisplay().getWidth(), input.game.getDisplay().getHeight(), 0);

        ShaderProgram hudShaderProgram = hud_shader;
        hudShaderProgram.bind();
        hudShaderProgram.setUniform("texture_sampler", 0);

        for(String meshId :input.scene.shaderblock_mesh_model_map.get(blockID).keySet()) {
            Mesh mesh = input.scene.meshID_mesh_map.get(meshId);

            for(String modelId : input.scene.shaderblock_mesh_model_map.get(blockID).get(meshId).keySet()) {
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
    }

    @Override
    public void cleanUp() {
        hud_shader.cleanUp();
    }
}
