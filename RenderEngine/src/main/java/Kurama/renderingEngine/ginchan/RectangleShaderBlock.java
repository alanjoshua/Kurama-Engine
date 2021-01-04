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
    public static final int BUFFER_SIZE_BYTES = MATRIX_SIZE_BYTES + VECTOR4F_SIZE_BYTES + FLOAT_SIZE_BYTES;

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

            var jointsDataInstancedBuffer = MemoryUtil.memAllocFloat(23);
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

        test = new Rectangle(null, "test");
        test.width = 100;
        test.height = 100;
        test.radii = new Vector(30,40,50,60);
        test.orientation = Quaternion.getAxisAsQuat(0,0, 1,0);
    }

    public void setupRectangleUniform(Matrix projectionViewMatrix, Vector radius, int width, int height, boolean hasTexture) {

        glBindBuffer(GL_UNIFORM_BUFFER, rectangleUniformBuffer);

        FloatBuffer temp = MemoryUtil.memAllocFloat(23);
        projectionViewMatrix.setValuesToBuffer(temp);
        radius.setValuesToBuffer(temp);
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
        shader.bind();

        var cam = input.scene.cameras.get(1);

        test.pos = new Vector(new float[]{cam.renderResolution.get(0)/4, cam.renderResolution.get(1)/4, 0});
        test.width = (int)cam.renderResolution.get(0)/2;
        test.height = (int)cam.renderResolution.get(1)/2;

        Quaternion rot = Quaternion.getAxisAsQuat(0,0,1,1);
        test.orientation = rot.multiply(test.orientation);

        Matrix ortho = Matrix.buildOrtho2D(0, input.game.getDisplay().windowResolution.get(0), input.game.getDisplay().windowResolution.get(1), 0);
        var mat = ortho.matMul(test.getObjectToWorldMatrix());

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, cam.renderBuffer.textureId);
        setupRectangleUniform(mat, test.radii, test.width, test.height, true);

        glDrawMeshTasksNV(0,1);
        shader.unbind();

        return null;
    }

    @Override
    public void cleanUp() {
        shader.cleanUp();
    }
}
