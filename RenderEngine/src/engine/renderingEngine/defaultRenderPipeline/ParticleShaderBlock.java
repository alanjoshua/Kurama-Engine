package engine.renderingEngine.defaultRenderPipeline;

import engine.Math.Matrix;
import engine.Mesh.Mesh;
import engine.particle.Particle;
import engine.renderingEngine.RenderBlock;
import engine.renderingEngine.RenderBlockInput;
import engine.shader.ShaderProgram;
import engine.utils.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.glActiveTexture;

public class ParticleShaderBlock extends RenderBlock {

    public static String particleShaderID = "particleShader";
    public ShaderProgram particleShader;

    public ParticleShaderBlock(String id) {
        super(id);
    }

    @Override
    public void setup(RenderBlockInput input) {
        particleShader = new ShaderProgram(particleShaderID);

        try {
            particleShader.createVertexShader("src/engine/renderingEngine/defaultShaders/ParticleVertexShader.glsl");
            particleShader.createFragmentShader("src/engine/renderingEngine/defaultShaders/ParticleFragmentShader.glsl");
            particleShader.link();

            particleShader.createUniform("projectionMatrix");
            particleShader.createUniform("modelViewMatrix");
            particleShader.createUniform("texture_sampler");

            particleShader.createUniform("texXOffset");
            particleShader.createUniform("texYOffset");
            particleShader.createUniform("numRows");
            particleShader.createUniform("numCols");
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void render(RenderBlockInput input) {
        glDepthMask(false);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);
        particleShader.bind();
        particleShader.setUniform("texture_sampler", 0);

        Matrix worldToCam = input.scene.camera.getWorldToCam();
        Matrix projectionMatrix = input.scene.camera.getPerspectiveProjectionMatrix();
        particleShader.setUniform("projectionMatrix",projectionMatrix);

        for(var generatorID: input.scene.shaderBlockID_particelGenID_map.get(blockID)) {
            var generator = input.scene.particleGenID_generator_map.get(generatorID);
            var baseParticle = generator.baseParticle;

            var camera = input.scene.camera;
            SortedMap<Float, Particle> sorted = new TreeMap<>(Collections.reverseOrder());
            generator.particles.stream().forEach(p -> {
                float dist = (float)Math.sqrt(Math.pow(camera.getPos().get(0) - p.pos.get(0), 2) +
                        Math.pow(camera.getPos().get(1) - p.pos.get(1), 2) +
                        Math.pow(camera.getPos().get(2) - p.pos.get(2), 2));
                sorted.put(dist, p);
            });
            generator.particles = new ArrayList<>(sorted.values());

            for(Mesh mesh: baseParticle.meshes) {

                if (mesh.materials.get(0).texture != null) {
                    glActiveTexture(GL_TEXTURE0);
                    glBindTexture(GL_TEXTURE_2D, mesh.materials.get(0).texture.getId());
                }

                mesh.initRender();

                for(var particle: generator.particles) {

                    var text = particle.meshes.get(0).materials.get(0).texture;

                    int col = particle.texPos % text.numCols;
                    int row = particle.texPos / text.numCols;
                    float textXOffset = (float) col / text.numCols;
                    float textYOffset = (float) row / text.numRows;

                    Logger.log("texPos:"+ particle.texPos + " texX: "+textXOffset+ " texY: "+textYOffset);

                    particleShader.setUniform("texXOffset", textXOffset);
                    particleShader.setUniform("texYOffset", textYOffset);
                    particleShader.setUniform("numCols", text.numCols);
                    particleShader.setUniform("numRows", text.numCols);

                    Matrix objectToWorld = particle.getObjectToWorldMatrix();
                    Matrix modelView = worldToCam.matMul(objectToWorld);

                    Matrix billboard = Matrix.getDiagonalMatrix(particle.scale).
                            addColumn(modelView.getColumn(3).
                            removeDimensionFromVec(3)).addRow(modelView.getRow(3));

                    particleShader.setUniform("modelViewMatrix", billboard);
                    mesh.render();
                }
                mesh.endRender();
            }

        }

        particleShader.unbind();
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(true);
    }

    @Override
    public void cleanUp() {

    }
}
