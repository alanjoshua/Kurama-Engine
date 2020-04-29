package ENED_Simulation;

import engine.GUI.Text;
import engine.HUD;
import engine.Math.Vector;
import engine.game.Game;

public class SimulationHUD extends HUD {

    public Text demoText;

    public SimulationHUD(Game game) {
        super(game);
        demoText = new Text(game, "Alan's 3D ENED Simulation", "textures/fontTexture.png", 16, 16, "text");
        hudElements.add(demoText);
        demoText.mesh.material.ambientColor = new Vector(new float[]{1,1f,1f,1});
    }

    public void tick() {
        demoText.setPos(new Vector(new float[]{game.getDisplay().getWidth() / 3,game.getDisplay().getHeight() / 3,0}));
//        demoText.setPos(new Vector(new float[]{100,game.getDisplay().getHeight() - 100,0}));
    }
}
