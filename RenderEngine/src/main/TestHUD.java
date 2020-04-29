package main;

import engine.GUI.Text;
import engine.HUD;
import engine.Math.Vector;
import engine.game.Game;

public class TestHUD extends HUD {

    public Text demoText;

    public TestHUD(Game game) {
        super(game);
        demoText = new Text(game, "Hello World", "textures/fontTexture.png", 16, 16, "text");
        hudElements.add(demoText);
    }

    public void tick() {
        demoText.setPos(new Vector(new float[]{100,game.getDisplay().getHeight() - 100,0}));
    }

}
