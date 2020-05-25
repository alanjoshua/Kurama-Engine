package main;

import engine.GUI.Text;
import engine.HUD;
import engine.Math.Vector;
import engine.font.FontTexture;
import engine.game.Game;

import java.awt.*;

public class TestHUD extends HUD {

    public Text demoText;
    Font FONT = new Font("Arial", Font.PLAIN, 25);
    String CHARSET = "ISO-8859-1";

    public TestHUD(Game game) {
        super(game);
        demoText = new Text(game, "Kurama Engine -alpha 2.0", new FontTexture(FONT,CHARSET), "sample");
        demoText.mesh.materials.get(0).ambientColor = new Vector(new float[]{1,1,1,0.5f});
        hudElements.add(demoText);
    }

    public void tick() {
        demoText.setPos(new Vector(new float[]{50,game.getDisplay().getHeight() - 50,0}));
    }

}
