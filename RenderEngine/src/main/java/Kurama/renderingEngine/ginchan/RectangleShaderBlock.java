package Kurama.renderingEngine.ginchan;

import Kurama.GUI.Rectangle;
import Kurama.Math.Matrix;
import Kurama.Math.Quaternion;
import Kurama.Math.Vector;
import Kurama.renderingEngine.*;
import Kurama.shader.ShaderProgram;
import Kurama.utils.Utils;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.glBindTexture;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15C.*;
import static org.lwjgl.opengl.GL30C.glBindBufferBase;
import static org.lwjgl.opengl.GL31C.GL_UNIFORM_BUFFER;
import static org.lwjgl.opengl.NVMeshShader.glDrawMeshTasksNV;

public class RectangleShaderBlock extends RenderBlock {

    private ShaderProgram shader;
    Rectangle test;
    int rectangleUniformBuffer;

    public static final int FLOAT_SIZE_BYTES = 4;
    public static final int VECTOR4F_SIZE_BYTES = 4 * FLOAT_SIZE_BYTES;
    public static final int MATRIX_SIZE_BYTES = 4 * VECTOR4F_SIZE_BYTES;

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

            rectangleUniformBuffer = glGenBuffers();
            glBindBuffer(GL_UNIFORM_BUFFER, rectangleUniformBuffer);

            var jointsDataInstancedBuffer = MemoryUtil.memAllocFloat(27);
            glBufferData(GL_UNIFORM_BUFFER, jointsDataInstancedBuffer, GL_DYNAMIC_DRAW);
            glBindBufferBase(GL_UNIFORM_BUFFER, 0, rectangleUniformBuffer);

            glBindBuffer(GL_UNIFORM_BUFFER, 0);
            MemoryUtil.memFree(jointsDataInstancedBuffer);

//            shader.createUniform("projectionViewMatrix");
//            shader.createUniform("rectangle");
            shader.createUniform("texture_sampler");
            shader.setUniform("texture_sampler", 0);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        test = new Rectangle(null, null,"test");
        test.width = 100;
        test.height = 100;
        test.radii = new Vector(30,40,50,60);
        test.orientation = Quaternion.getAxisAsQuat(0,0, 1,0);
    }

    public void setupRectangleUniform(Matrix projectionViewMatrix, Vector radius, int width, int height, boolean hasTexture,
                                      Vector color) {

        glBindBuffer(GL_UNIFORM_BUFFER, rectangleUniformBuffer);

        FloatBuffer temp = MemoryUtil.memAllocFloat(27);
        projectionViewMatrix.setValuesToBuffer(temp);

        if(radius != null) {
            radius.setValuesToBuffer(temp);
        }
        else {
            temp.put(new float[]{0,0,0,0});
        }

        if (color != null) {
            color.setValuesToBuffer(temp);
        }
        else {
            temp.put(new float[]{0,0,0,0});
        }
        temp.put(width);
        temp.put(height);
        temp.put(hasTexture?1f:0f);
        temp.flip();
        glBufferSubData(GL_UNIFORM_BUFFER, 0, temp);
        MemoryUtil.memFree(temp);

        glBindBuffer(GL_UNIFORM_BUFFER, 0);
    }

    @Override
    public RenderBlockOutput render(RenderBlockInput input) {

        GUIComponentRenderInput inp = (GUIComponentRenderInput) input;
        Rectangle masterComponent = (Rectangle)inp.component;
        Matrix ortho = Matrix.buildOrtho2D(0, input.game.getDisplay().windowResolution.get(0), input.game.getDisplay().windowResolution.get(1), 0);

        shader.bind();

        var mat = ortho.matMul(masterComponent.getObjectToWorldMatrix());

        if(masterComponent.texture != null) {
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, masterComponent.texture.getId());
        }

        setupRectangleUniform(mat, masterComponent.radii, masterComponent.width, masterComponent.height,
                masterComponent.texture== null?false:true, masterComponent.color);

        glDrawMeshTasksNV(0,1);

        shader.unbind();

        return null;
    }

    @Override
    public void cleanUp() {
        shader.cleanUp();
    }
}
