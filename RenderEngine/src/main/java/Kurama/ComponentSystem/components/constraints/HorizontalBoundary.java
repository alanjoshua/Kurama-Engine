package Kurama.ComponentSystem.components.constraints;

import Kurama.ComponentSystem.components.Component;
import Kurama.game.Game;

public class HorizontalBoundary extends Boundary {

    public HorizontalBoundary(Game game, Component parent, String identifier) {
        super(game, parent, identifier);
        height = 10;
    }
}
