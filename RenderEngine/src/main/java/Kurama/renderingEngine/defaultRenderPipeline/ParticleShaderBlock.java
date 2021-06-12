package Kurama.renderingEngine.defaultRenderPipeline;

import Kurama.ComponentSystem.components.model.Model;
import Kurama.Math.Matrix;
import Kurama.Mesh.InstancedUtils;
import Kurama.Mesh.Mesh;
import Kurama.game.Game;
import Kurama.particle.ParticleGenerator;
import Kurama.renderingEngine.CurrentCameraBlockData;
import Kurama.renderingEngine.RenderPipeline;
import Kurama.renderingEngine.RenderPipelineData;
import Kurama.shader.ShaderProgram;

import java.util.*;

import static org.lwjgl.opengl.GL15.*;

public class ParticleShaderBlock extends RenderPipeline {

    private static String particleShaderID = "particleShader";
    private ShaderProgram particleShader;

    public ParticleShaderBlock(Game game, RenderPipeline parentPipeline, String pipelineID) {
        super(game, parentPipeline, pipelineID);
    }

    @Override
    public void setup(RenderPipelineData input) {
        particleShader = new ShaderProgram(particleShaderID);

        try {
            particleShader.createVertexShader("src/main/java/Kurama/renderingEngine/defaultRenderPipeline/shaders/ParticleVertexShader.glsl");
            particleShader.createFragmentShader("src/main/java/Kurama/renderingEngine/defaultRenderPipeline/shaders/ParticleFragmentShader.glsl");
            particleShader.link();

            particleShader.createUniform("projectionMatrix");
            particleShader.createUniform("texture_sampler");

            particleShader.createUniform("numRows");
            particleShader.createUniform("numCols");

            particleShader.setUniform("texture_sampler", 0);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void renderChunk(List<Model> chunk, Mesh inst_mesh, List<Matrix> objectToCam, int offset) {
        inst_mesh.instanceDataBuffer.clear();

        int particleCount = 0;
        for(var particle: chunk) {
            Matrix modelView = objectToCam.get(offset+particleCount);

            Matrix billboard = Matrix.getDiagonalMatrix(particle.getScale()).
                    addColumn(modelView.getColumn(3).
                            removeDimensionFromVec(3)).addRow(modelView.getRow(3));

            var mat = particle.materials.get(inst_mesh.meshIdentifier).get(0);
            var tex = mat.texture;
            int texPos = particle.matAtlasOffset.get(inst_mesh.meshIdentifier).get(0);
            int col = texPos % tex.numCols;
            int row = texPos / tex.numCols;
            float textXOffset = (float) col / tex.numCols;
            float textYOffset = (float) row / tex.numRows;

            billboard.setValuesToBuffer(inst_mesh.instanceDataBuffer);

            inst_mesh.instanceDataBuffer.put(textXOffset);
            inst_mesh.instanceDataBuffer.put(textYOffset);
            for(int counter = 0; counter < 2; counter++) {
                inst_mesh.instanceDataBuffer.put(0);
            }
            particleCount++;
        }

        inst_mesh.instanceDataBuffer.flip();

        glBindBuffer(GL_ARRAY_BUFFER, inst_mesh.instanceDataVBO);
        glBufferData(GL_ARRAY_BUFFER, inst_mesh.instanceDataBuffer, GL_DYNAMIC_DRAW);

        ((DefaultRenderPipeline)parentPipeline).renderInstanced(inst_mesh, chunk.size());
    }

    public void renderGenerator(ParticleGenerator generator, Matrix worldToCam, boolean curShouldCull, int currCull) {

        var baseParticle = generator.baseParticle;

        SortedMap<Float, List> sorted = new TreeMap<>(Collections.reverseOrder());

        for(var p: generator.particles) {
            if(p.shouldRender && p.isInsideFrustum) {

                var objectToCam = worldToCam.matMul(p.getObjectToWorldMatrix());
                var trans = objectToCam.matMul(p.getPos().append(1)).toVector();
                float dist = -trans.get(2);
                var store = new ArrayList();
                store.add(p);
                store.add(objectToCam);
                sorted.put(dist, store);
            }
        }

        List<Model> sortedPartices = new ArrayList<>();
        List<Matrix> sorted_objectToCam = new ArrayList<>();

        for(var m: sorted.values()) {
            sortedPartices.add((Model)m.get(0));
            sorted_objectToCam.add((Matrix)m.get(1));
        }

        var sortedParticles_list = new ArrayList<>(sortedPartices);

        for(int i = 0;i < baseParticle.meshes.size();i++) {
            var mesh = baseParticle.meshes.get(i);

            if (curShouldCull != mesh.shouldCull) {
                if(mesh.shouldCull) {
                    glEnable(GL_CULL_FACE);
                }
                else {
                    glDisable(GL_CULL_FACE);
                }
                curShouldCull = mesh.shouldCull;
            }

            if(currCull != mesh.cullmode) {
                glCullFace(mesh.cullmode);
                currCull = mesh.cullmode;
            }

            if(!(mesh.isInstanced)) {
                throw new IllegalArgumentException("A particle has to be instanced");
            }

            var inst_mesh = mesh;

            if (inst_mesh.materials.get(0).texture != null) {
                glActiveTexture(GL_TEXTURE0);
                glBindTexture(GL_TEXTURE_2D, inst_mesh.materials.get(0).texture.getId());
            }

            particleShader.setUniform("numCols", inst_mesh.materials.get(0).texture.numCols);
            particleShader.setUniform("numRows", inst_mesh.materials.get(0).texture.numRows);

            ((DefaultRenderPipeline)parentPipeline).initRender(mesh);

            int modelsProcessedSoFar = 0;
            var chunks = InstancedUtils.getRenderChunks(sortedParticles_list, inst_mesh.instanceChunkSize);
            for(var chunk: chunks) {
                renderChunk(chunk, inst_mesh, sorted_objectToCam, modelsProcessedSoFar);
                modelsProcessedSoFar+=chunk.size();
            }
            ((DefaultRenderPipeline)parentPipeline).endRender(mesh);
        }

    }

    @Override
    public RenderPipelineData render(RenderPipelineData input) {

        if(input.scene.shaderBlockID_particelGenID_map.get(pipelineID) == null) {
            return null;
        }

        CurrentCameraBlockData inp = (CurrentCameraBlockData) input;
        var camera = inp.camera;

        boolean curShouldCull = true;
        int currCull = GL_BACK;

        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        particleShader.bind();

        Matrix worldToCam = camera.getWorldToObject();
        Matrix projectionMatrix = camera.getPerspectiveProjectionMatrix();
        particleShader.setUniform("projectionMatrix", projectionMatrix);

        for (var generatorID : input.scene.shaderBlockID_particelGenID_map.get(pipelineID)) {
            var generator = input.scene.particleGenID_generator_map.get(generatorID);
            renderGenerator(generator, worldToCam, curShouldCull, currCull);
        }

        particleShader.unbind();

        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        return input;
    }

    @Override
    public void cleanUp() {
        particleShader.cleanUp();
    }
}
