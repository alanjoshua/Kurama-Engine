package Kurama.ComponentSystem.components.constraints;

import Kurama.ComponentSystem.components.Component;
import Kurama.game.Game;

public class VerticalBoundary extends Boundary {

    public VerticalBoundary(Game game, Component parent, String identifier) {
        super(game, parent, identifier);
        width = 10;
    }
}
