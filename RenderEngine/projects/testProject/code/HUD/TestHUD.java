package HUD;

import engine.GUI.Text;
import engine.Math.Vector;
import engine.font.FontTexture;
import engine.game.Game;
import engine.model.HUD;
import engine.renderingEngine.defaultRenderPipeline.DefaultRenderPipeline;

import java.awt.*;
import java.util.Arrays;

public class TestHUD extends HUD {

    public Text engineInfo;
    public Text FPS;
    Font FONT = new Font("Arial", Font.PLAIN, 25);
    String CHARSET = "ISO-8859-1";
    FontTexture engineTextFont = new FontTexture(FONT,CHARSET);
    FontTexture fpsTextFont = new FontTexture(new Font("Arial", Font.PLAIN, 14),CHARSET);

    public TestHUD(Game game) {
        super(game);

        engineInfo = new Text(game, "Kurama Engine -alpha 2.1", engineTextFont, "HUD_text");
        engineInfo.meshes.get(0).meshIdentifier = "hud_text_mesh";
        engineInfo.meshes.get(0).materials.get(0).ambientColor = new Vector(new float[]{1,1,1,0.5f});
        engineInfo.setPos(new Vector(new float[]{game.getDisplay().getWidth() - 400,game.getDisplay().getHeight() - 50,0}));

        hudElements.add(engineInfo);
        game.scene.addModel(engineInfo, Arrays.asList(new String[]{DefaultRenderPipeline.hudShaderBlockID}));

        FPS = new Text(game, "FPS: "+game.displayFPS, fpsTextFont, "FPS");
        FPS.meshes.get(0).meshIdentifier = "fps_mesh";
        FPS.meshes.get(0).materials.get(0).ambientColor = new Vector(new float[]{1,1,1,0.5f});
        FPS.setPos(new Vector(new float[]{10,10,0}));

        hudElements.add(FPS);
        game.scene.addModel(FPS, Arrays.asList(new String[]{DefaultRenderPipeline.hudShaderBlockID}));
    }

    public void tick() {

        engineInfo.setPos(new Vector(new float[]{game.getDisplay().getWidth() - 400,game.getDisplay().getHeight() - 50,0}));

        game.scene.removeModel(FPS);
        hudElements.remove(1);
        FPS.meshes.forEach(m -> m.cleanUp(false));

        FPS = new Text(game, "FPS: "+game.displayFPS, fpsTextFont, "FPS");
        FPS.meshes.get(0).meshIdentifier = "fps_mesh";
        FPS.meshes.get(0).materials.get(0).ambientColor = new Vector(new float[]{1,1,1,0.5f});
        FPS.setPos(new Vector(new float[]{10,10,0}));

        hudElements.add(FPS);
        game.scene.addModel(FPS, Arrays.asList(new String[]{DefaultRenderPipeline.hudShaderBlockID}));

    }

}
