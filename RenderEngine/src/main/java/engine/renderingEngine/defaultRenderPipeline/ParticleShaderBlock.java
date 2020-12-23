package engine.renderingEngine.defaultRenderPipeline;

import engine.Math.Matrix;
import engine.Mesh.InstancedMesh;
import engine.model.Model;
import engine.renderingEngine.RenderBlock;
import engine.renderingEngine.RenderBlockInput;
import engine.shader.ShaderProgram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.lwjgl.opengl.GL15.*;

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
            particleShader.createVertexShader("src/main/java/engine/renderingEngine/defaultShaders/ParticleVertexShader.glsl");
            particleShader.createFragmentShader("src/main/java/engine/renderingEngine/defaultShaders/ParticleFragmentShader.glsl");
            particleShader.link();

            particleShader.createUniform("projectionMatrix");
            particleShader.createUniform("texture_sampler");

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

        if(input.scene.shaderBlockID_particelGenID_map.get(blockID) == null) {
            return;
        }

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
            SortedMap<Float, Model> sorted = new TreeMap<>(Collections.reverseOrder());
            generator.particles.stream().forEach(p -> {
                if(p.shouldRender) {
                    float dist = (float) Math.sqrt(Math.pow(camera.getPos().get(0) - p.pos.get(0), 2) +
                            Math.pow(camera.getPos().get(1) - p.pos.get(1), 2) +
                            Math.pow(camera.getPos().get(2) - p.pos.get(2), 2));
                    sorted.put(dist, p);
                }
            });
            generator.particles = new ArrayList<>(sorted.values());

            for(int i = 0;i < baseParticle.meshes.size();i++) {
                var mesh = baseParticle.meshes.get(i);

                if(!(mesh instanceof InstancedMesh)) {
                    throw new IllegalArgumentException("A particle has to be instanced");
                }

                var inst_mesh = (InstancedMesh)mesh;

                if (inst_mesh.materials.get(i).texture != null) {
                    glActiveTexture(GL_TEXTURE0);
                    glBindTexture(GL_TEXTURE_2D, inst_mesh.materials.get(0).texture.getId());
                }

                particleShader.setUniform("numCols", inst_mesh.materials.get(0).texture.numCols);
                particleShader.setUniform("numRows", inst_mesh.materials.get(0).texture.numRows);

                mesh.initRender();

                var chunks = InstancedMesh.getRenderChunks(generator.particles, inst_mesh.instanceChunkSize);
                for(var chunk: chunks) {

                    inst_mesh.instanceDataBuffer.clear();

                    for(var particle: chunk) {
                        Matrix objectToWorld = particle.getObjectToWorldMatrix();
                        Matrix modelView = worldToCam.matMul(objectToWorld);

                        Matrix billboard = Matrix.getDiagonalMatrix(particle.scale).
                                addColumn(modelView.getColumn(3).
                                        removeDimensionFromVec(3)).addRow(modelView.getRow(3));

                        var mat = particle.materials.get(inst_mesh.meshIdentifier).get(0);
                        var tex = mat.texture;
                        int texPos = particle.matAtlasOffset.get(inst_mesh.meshIdentifier).get(0);
                        int col = texPos % tex.numCols;
                        int row = texPos / tex.numCols;
                        float textXOffset = (float) col / tex.numCols;
                        float textYOffset = (float) row / tex.numRows;

                        billboard.setValuesToFloatBuffer(inst_mesh.instanceDataBuffer);

                        inst_mesh.instanceDataBuffer.put(textXOffset);
                        inst_mesh.instanceDataBuffer.put(textYOffset);
                        for(int counter = 0; counter < 6; counter++) {
                            inst_mesh.instanceDataBuffer.put(0);
                        }
                    }

                    inst_mesh.instanceDataBuffer.flip();

                    glBindBuffer(GL_ARRAY_BUFFER, inst_mesh.instanceDataVBO);
                    glBufferData(GL_ARRAY_BUFFER, inst_mesh.instanceDataBuffer, GL_DYNAMIC_DRAW);

                    inst_mesh.render(chunk.size());
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
