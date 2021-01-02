package Kurama.renderingEngine.ginchan;

import Kurama.GUI.Rectangle;
import Kurama.Math.Matrix;
import Kurama.Math.Quaternion;
import Kurama.Math.Vector;
import Kurama.renderingEngine.RenderBlock;
import Kurama.renderingEngine.RenderBlockInput;
import Kurama.renderingEngine.RenderPipeline;
import Kurama.shader.ShaderProgram;
import Kurama.utils.Utils;

import static org.lwjgl.opengl.NVMeshShader.glDrawMeshTasksNV;

public class RectangleShaderBlock extends RenderBlock {

    private ShaderProgram shader;
    Rectangle test;

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

            shader.createUniform("projectionViewMatrix");
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        test = new Rectangle(null, "test");
        test.width = 100;
        test.height = 100;
        test.orientation = Quaternion.getAxisAsQuat(0,0, 1,0);
    }

    @Override
    public void render(RenderBlockInput input) {
        shader.bind();

        test.pos = new Vector(new float[]{input.game.getDisplay().windowResolution.get(0)/2, input.game.getDisplay().windowResolution.get(1)/2, 0});
        Quaternion rot = Quaternion.getAxisAsQuat(0,0,1,1);
        test.orientation = rot.multiply(test.orientation);

        Matrix ortho = Matrix.buildOrtho2D(0, input.game.getDisplay().windowResolution.get(0), input.game.getDisplay().windowResolution.get(1), 0);
        var mat = ortho.matMul(test.getObjectToWorldMatrix());
        shader.setUniform("projectionViewMatrix", mat);

        glDrawMeshTasksNV(0,1);
        shader.unbind();
    }

    @Override
    public void cleanUp() {
        shader.cleanUp();
    }
}
