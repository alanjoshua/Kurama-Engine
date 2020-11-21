package RenderPipeline;

import engine.Math.Matrix;
import engine.model.Model;
import engine.renderingEngine.RenderBlockInput;
import engine.shader.ShaderProgram;
import engine.utils.Utils;

import java.io.File;
import java.nio.file.Path;

public class HUD_ShaderBlock extends engine.renderingEngine.RenderBlock {

    String hud_shader_id = "hud_shader";
    public ShaderProgram hud_shader;

    public HUD_ShaderBlock(String id) {
        super(id);
    }

    @Override
    public void setup(RenderBlockInput input) {

        try {

            Path thisFilePath = Utils.getClassPath("HUD_ShaderBlock.java");
            File shadersDir = thisFilePath.getParent().getParent().getParent().toFile();

            hud_shader = new ShaderProgram(hud_shader_id);

            hud_shader.createVertexShader(shadersDir.getAbsolutePath()+"/Shaders/HUDVertexShader.glsl");
            hud_shader.createFragmentShader(shadersDir.getAbsolutePath()+"/Shaders/HUDFragmentShader.glsl");
            hud_shader.link();

            // Create uniforms for Orthographic-model projection matrix and base colour
            hud_shader.createUniform("texture_sampler");
            hud_shader.createUniform("projModelMatrix");
            hud_shader.createUniform("color");
            hud_shader.createUniform("shouldGreyScale");
            hud_shader.createUniform("shouldLinearizeDepth");

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

            for(String modelId : input.scene.shaderblock_mesh_model_map.get(blockID).get(meshId).keySet()) {
                Model m = input.scene.modelID_model_map.get(modelId);

                if (m.shouldRender) {
                    // Set orthographic and model matrix for this HUD item
                    Matrix projModelMatrix = ortho.matMul((m.getObjectToWorldMatrix()));
                    hudShaderProgram.setUniform("projModelMatrix", projModelMatrix);
                    hudShaderProgram.setUniform("color", m.mesh.materials.get(0).ambientColor);

                    hudShaderProgram.setUniform("shouldGreyScale", m.shouldGreyScale ? 1 : 0);
                    hudShaderProgram.setUniform("shouldLinearizeDepth", m.shouldLinearizeDepthInHUD ? 1 : 0);

                    m.mesh.initToEndFullRender(0);
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
