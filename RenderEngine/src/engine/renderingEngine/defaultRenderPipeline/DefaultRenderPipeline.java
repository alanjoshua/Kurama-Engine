package engine.renderingEngine.defaultRenderPipeline;

import engine.game.Game;
import engine.renderingEngine.RenderBlockInput;
import engine.scene.Scene;

public class DefaultRenderPipeline extends engine.renderingEngine.RenderPipeline {

    public static String sceneShaderBlockID = "sceneShaderBlock";
    public static String hudShaderBlockID = "hudShaderBlock";
    public static String skyboxShaderBlockID = "skyboxShaderBlock";
    public static String particleShaderBlockID = "particleShaderBlock";

    SceneShaderBlock sceneShaderBlock = new SceneShaderBlock(sceneShaderBlockID);
    HUD_ShaderBlock hudShaderBlock = new HUD_ShaderBlock(hudShaderBlockID);
    SkyboxShaderBlock skyboxShaderBlock = new SkyboxShaderBlock(skyboxShaderBlockID);
    ParticleShaderBlock particleShaderBlock = new ParticleShaderBlock(particleShaderBlockID);

    public DefaultRenderPipeline(Game game) {
        super(game);

    }

    @Override
    public void setup(Scene scene) {
        sceneShaderBlock.setup(new RenderBlockInput(scene, game));
        skyboxShaderBlock.setup(new RenderBlockInput(scene, game));
        hudShaderBlock.setup(new RenderBlockInput(scene, game));
        particleShaderBlock.setup(new RenderBlockInput(scene, game));

        renderBlockID_renderBlock_map.put(sceneShaderBlockID, sceneShaderBlock);
        renderBlockID_renderBlock_map.put(skyboxShaderBlockID, skyboxShaderBlock);
        renderBlockID_renderBlock_map.put(hudShaderBlockID, hudShaderBlock);
        renderBlockID_renderBlock_map.put(particleShaderBlockID, particleShaderBlock);
    }

    @Override
    public void render(Scene scene) {
        sceneShaderBlock.render(new RenderBlockInput(scene, game));
        skyboxShaderBlock.render(new RenderBlockInput(scene, game));

        //Hud should be rendered at last, or else text would have background
        hudShaderBlock.render(new RenderBlockInput(scene, game));

        particleShaderBlock.render(new RenderBlockInput(scene, game));
    }

    @Override
    public void cleanUp() {
        sceneShaderBlock.cleanUp();
        skyboxShaderBlock.cleanUp();
        hudShaderBlock.cleanUp();
        particleShaderBlock.cleanUp();
    }
}
