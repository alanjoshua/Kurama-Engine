package HUD;

import engine.GUI.Text;
import engine.Math.Vector;
import engine.Mesh.Mesh;
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

        var res = Text.buildTextMesh("Kurama Engine -alpha 2.1", engineTextFont);
        var engineInfo_mesh = (Mesh)res.get(0);
        var width = (Float)res.get(1);
        engineInfo_mesh.meshIdentifier = "hud_text_mesh";
        engineInfo_mesh.materials.get(0).ambientColor = new Vector(new float[]{1,1,1,0.5f});
        engineInfo = new Text(game, "Kurama Engine -alpha 2.1", engineInfo_mesh,engineTextFont, width, "engineInfo");
        engineInfo.setPos(new Vector(new float[]{game.getDisplay().getWidth() - 400,game.getDisplay().getHeight() - 50,0}));
        hudElements.add(engineInfo);
        game.scene.addModel(engineInfo, Arrays.asList(new String[]{DefaultRenderPipeline.hudShaderBlockID}));


        res = Text.buildTextMesh("FPS: "+game.displayFPS, fpsTextFont);
        var fps_mesh = (Mesh)res.get(0);
        width = (Float)res.get(1);
        fps_mesh.meshIdentifier = "fps_mesh";
        fps_mesh.materials.get(0).ambientColor = new Vector(new float[]{1,1,1,0.5f});
        fps_mesh.materials.get(0).matName = "fpsMat";
        FPS = new Text(game, "FPS: "+game.displayFPS, fps_mesh, fpsTextFont, width, "FPS");
        FPS.setPos(new Vector(new float[]{10,10,0}));

        hudElements.add(FPS);
        game.scene.addModel(FPS, Arrays.asList(new String[]{DefaultRenderPipeline.hudShaderBlockID}));
    }

    public void tick() {

        engineInfo.setPos(new Vector(new float[]{game.getDisplay().getWidth() - 400,game.getDisplay().getHeight() - 50,0}));

        game.scene.removeModel(FPS);
        hudElements.remove(1);
        FPS.meshes.forEach(m -> m.cleanUp(false));

        var res = Text.buildTextMesh("FPS: "+game.displayFPS, fpsTextFont);
        var fps_mesh = (Mesh)res.get(0);
        var width = (Float)res.get(1);
        fps_mesh.meshIdentifier = "fps_mesh";
        fps_mesh.materials.get(0).ambientColor = new Vector(new float[]{1,1,1,0.5f});
        fps_mesh.materials.get(0).matName = "fpsMat";
        FPS = new Text(game, "FPS: "+game.displayFPS, fps_mesh, fpsTextFont, width, "FPS");
        FPS.setPos(new Vector(new float[]{10,10,0}));

        hudElements.add(FPS);
        game.scene.addModel(FPS, Arrays.asList(new String[]{DefaultRenderPipeline.hudShaderBlockID}));

    }

}
