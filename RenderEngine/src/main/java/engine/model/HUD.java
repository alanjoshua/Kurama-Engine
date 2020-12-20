package engine.model;

import engine.game.Game;

import java.util.ArrayList;
import java.util.List;

public class HUD {

    public List<Model> hudElements = null;
    protected Game game;

    public HUD(Game game) {
        this.game = game;
        hudElements = new ArrayList<>();
    }

    public void tick() {

    }

}
