package ENED_Simulation;

import engine.GUI.Text;
import engine.HUD;
import engine.Math.Vector;
import engine.font.FontTexture;
import engine.game.Game;

import java.awt.*;

public class SimulationHUD extends HUD {

    public Text demoText;
    Font FONT = new Font("Consolas", Font.PLAIN, 50);
    String CHARSET = "ISO-8859-1";

    public SimulationHUD(Game game) {
        super(game);
        demoText = new Text(game, "Alan's 3D ENED Simulation", new FontTexture(FONT,CHARSET), "text");
        hudElements.add(demoText);
        demoText.mesh.material.ambientColor = new Vector(new float[]{1,1f,1f,1});
    }

    public void tick() {
        demoText.setPos(new Vector(new float[]{game.getDisplay().getWidth()/2 - demoText.width/2,game.getDisplay().getHeight()/2 - demoText.fontTexture.height/2,0}));
    }
}