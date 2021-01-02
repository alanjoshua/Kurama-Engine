package Kurama.renderingEngine.ginchan;

import Kurama.renderingEngine.RenderBlock;
import Kurama.renderingEngine.RenderBlockInput;
import Kurama.renderingEngine.RenderPipeline;
import Kurama.shader.ShaderProgram;
import Kurama.utils.Utils;

import static org.lwjgl.opengl.NVMeshShader.glDrawMeshTasksNV;

public class RectangleShaderBlock extends RenderBlock {

    private ShaderProgram shader;

    public RectangleShaderBlock(String id, RenderPipeline pipeline) {
        super(id, pipeline);
    }

    @Override
    public void setup(RenderBlockInput input) {
        shader = new ShaderProgram(Utils.getUniqueID());

        try {
            shader.createMeshShader("src/main/java/Kurama/renderingEngine/ginchan/shaders/RectangleMeshShader.glsl");
            shader.createFragmentShader("src/main/java/Kurama/renderingEngine/ginchan/shaders/RectangleFragmentShader.glsl");
            shader.link();
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    @Override
    public void render(RenderBlockInput input) {
        shader.bind();
        glDrawMeshTasksNV(0,1);
        shader.unbind();
    }

    @Override
    public void cleanUp() {
        shader.cleanUp();
    }
}
