package engine.renderingEngine.defaultRenderPipeline;

import engine.Math.Matrix;
import engine.Mesh.Mesh;
import engine.renderingEngine.RenderBlock;
import engine.renderingEngine.RenderBlockInput;
import engine.shader.ShaderProgram;

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

            for(Mesh mesh: baseParticle.meshes) {

                if (mesh.materials.get(0).texture != null) {
                    glActiveTexture(GL_TEXTURE0);
                    glBindTexture(GL_TEXTURE_2D, mesh.materials.get(0).texture.getId());
                }

                mesh.initRender();
                for(var particle: generator.particles) {
//                    Logger.log("particle scale: "+particle.scale.toString());
                    Matrix objectToWorld = particle.getObjectToWorldMatrix();
                    particleShader.setUniform("modelViewMatrix", worldToCam.matMul(objectToWorld));
//                    mesh.initToEndFullRender(0);
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
