package Kurama.renderingEngine.ginchan;

import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.components.Rectangle;
import Kurama.Math.Matrix;
import Kurama.Math.Vector;
import Kurama.game.Game;
import Kurama.renderingEngine.GUIComponentRenderData;
import Kurama.renderingEngine.RenderPipeline;
import Kurama.renderingEngine.RenderPipelineData;
import Kurama.shader.ShaderProgram;
import Kurama.utils.Utils;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15C.*;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;
import static org.lwjgl.opengl.GL44C.GL_DYNAMIC_STORAGE_BIT;
import static org.lwjgl.opengl.GL45C.glNamedBufferStorage;
import static org.lwjgl.opengl.GL45C.glNamedBufferSubData;
import static org.lwjgl.opengl.NVMeshShader.glDrawMeshTasksNV;

public class RectangleShaderBlock extends RenderPipeline {

    private ShaderProgram shader;
    int rectangleUniformBuffer;
    int bufferBaseInd = 2;
    FloatBuffer buffer;
    int currentNumGUIComps = 1;  //default
    int previousNumGUIComps = currentNumGUIComps;

    public static final int FLOAT_SIZE_BYTES = 4;
    public static final int VECTOR4F_SIZE_BYTES = 4 * FLOAT_SIZE_BYTES;
    public static final int MATRIX_SIZE_BYTES = 4 * VECTOR4F_SIZE_BYTES;
    public static final int INSTANCE_SIZE = 50;

    public RectangleShaderBlock(Game game, RenderPipeline parentPipeline, String pipelineID) {
        super(game, parentPipeline, pipelineID);
    }

    @Override
    public void setup(RenderPipelineData input) {
        shader = new ShaderProgram(Utils.getUniqueID());

        try {
            shader.createMeshShader("src/main/java/Kurama/renderingEngine/ginchan/shaders/RectangleMeshShader.glsl");
            shader.createFragmentShader("src/main/java/Kurama/renderingEngine/ginchan/shaders/RectangleFragmentShader.glsl");
            shader.link();

            rectangleUniformBuffer = glGenBuffers();
//            glBindBuffer(GL_UNIFORM_BUFFER, rectangleUniformBuffer);
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, rectangleUniformBuffer);

            buffer = MemoryUtil.memAllocFloat(INSTANCE_SIZE * currentNumGUIComps);

            glNamedBufferStorage(rectangleUniformBuffer, buffer, GL_DYNAMIC_STORAGE_BIT);
            GL30.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, bufferBaseInd, rectangleUniformBuffer);
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
//
//            glBufferData(GL_UNIFORM_BUFFER, tempBuffer, GL_DYNAMIC_DRAW);
//            glBindBufferBase(GL_UNIFORM_BUFFER, bufferBaseInd, rectangleUniformBuffer);
//            glBindBuffer(GL_UNIFORM_BUFFER, 0);

            shader.createUniform("texture_sampler");
            shader.setUniform("texture_sampler", 0);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    public void setupRectangleUniform(Matrix projectionViewMatrix, Vector radius, int width, int height, boolean hasTexture,
                                      Vector color, Vector overlayColor, float alphaMaskSoFar, Rectangle rect, FloatBuffer buffer) {

        buffer = MemoryUtil.memAllocFloat(INSTANCE_SIZE);
        projectionViewMatrix.setValuesToBuffer(buffer);

        if(radius != null) {
            radius.setValuesToBuffer(buffer);
        }
        else {
            buffer.put(new float[]{0,0,0,0});
        }

        if (color != null) {
            color.setValuesToBuffer(buffer);
        }
        else {
            buffer.put(new float[]{0,0,0,0});
        }

        if(overlayColor != null) {
            overlayColor.setValuesToBuffer(buffer);
        }
        else {
            buffer.put(new float[]{0,0,0,0});
        }

        rect.texUL.setValuesToBuffer(buffer);
        rect.texBL.setValuesToBuffer(buffer);
        rect.texUR.setValuesToBuffer(buffer);
        rect.texBR.setValuesToBuffer(buffer);

        buffer.put(width);
        buffer.put(height);
        buffer.put(0f);
        buffer.put(0f);

        buffer.put(hasTexture?1f:0f);
        buffer.put(alphaMaskSoFar);
//
        buffer.flip();
        glNamedBufferSubData(rectangleUniformBuffer, 0, buffer);
        MemoryUtil.memFree(buffer);
    }

    @Override
    public RenderPipelineData render(RenderPipelineData input) {

        GUIComponentRenderData inp = (GUIComponentRenderData) input;
        var masterComponent = inp.component;

        Matrix ortho = input.game.getMasterWindow().getOrthoProjection();

        shader.bind();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, rectangleUniformBuffer);
        glActiveTexture(GL_TEXTURE0);

        currentNumGUIComps = 0;
        countNumCompsToRender(masterComponent);  //sets currentNumGUIComps

        if(currentNumGUIComps > previousNumGUIComps) {
//            currentNumGUIComps +=10;
            MemoryUtil.memFree(buffer);
            buffer = MemoryUtil.memAllocFloat(INSTANCE_SIZE * currentNumGUIComps);
        }
        previousNumGUIComps = currentNumGUIComps;
        buffer.rewind();

        try {
            recursiveRenderSetup(masterComponent, ortho, new Vector(0, 0, 0, 0), 1, buffer);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

//        buffer.flip();
//        glNamedBufferSubData(rectangleUniformBuffer, 0, buffer);
//        glDrawMeshTasksNV(0, currentNumGUIComps);

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        shader.unbind();
        return input;
    }

    public void countNumCompsToRender(Component masterComponent) {
        if(!masterComponent.shouldTickRenderGroup) {
            return;
        }

        if(masterComponent.isContainerVisible && masterComponent instanceof Rectangle) {
            currentNumGUIComps++;
        }

        for(var child: masterComponent.children) {
            countNumCompsToRender(child);
        }
    }

    // This only render rectangle components
    public void recursiveRenderSetup(Component masterComponent, Matrix ortho, Vector colorSoFar, float alphaMaskSoFar, FloatBuffer buffer) {

        if(!masterComponent.shouldTickRenderGroup) {
            return;
        }

        if(masterComponent.isContainerVisible && masterComponent instanceof Rectangle) {

            var mat = ortho.matMul(masterComponent.getObjectToWorldMatrix());
            if(masterComponent.overlayColor != null) {
                colorSoFar = masterComponent.overlayColor.add(colorSoFar);
            }
            alphaMaskSoFar = alphaMaskSoFar*masterComponent.alphaMask;

            if (masterComponent.texture != null) {
                glBindTexture(GL_TEXTURE_2D, masterComponent.texture.getId());
            }
            setupRectangleUniform(mat, ((Rectangle)masterComponent).radii, masterComponent.getWidth(), masterComponent.getHeight(),
                    masterComponent.texture == null ? false : true, masterComponent.color, colorSoFar, alphaMaskSoFar, (Rectangle) masterComponent, buffer);

            glDrawMeshTasksNV(0, 1);
        }

//        var local = masterComponent.getObjectToWorldNoScale();
//        var nextParent = parentTrans.matMul(local);

        for(var child: masterComponent.children) {
            recursiveRenderSetup(child, ortho, colorSoFar, alphaMaskSoFar, buffer);
        }

    }

    @Override
    public void cleanUp() {
        shader.cleanUp();
    }
}
