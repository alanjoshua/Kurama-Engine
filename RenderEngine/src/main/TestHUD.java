package main;

import engine.GUI.Text;
import engine.HUD;
import engine.Math.Vector;
import engine.font.FontTexture;
import engine.game.Game;

import java.awt.*;

public class TestHUD extends HUD {

    public Text demoText;
    Font FONT = new Font("Arial", Font.PLAIN, 40);
    String CHARSET = "ISO-8859-1";

    public TestHUD(Game game) {
        super(game);
        demoText = new Text(game, "Hello World", new FontTexture(FONT,CHARSET), "text");
        hudElements.add(demoText);
    }

    public void tick() {
        demoText.setPos(new Vector(new float[]{100,game.getDisplay().getHeight() - 100,0}));
    }

}
