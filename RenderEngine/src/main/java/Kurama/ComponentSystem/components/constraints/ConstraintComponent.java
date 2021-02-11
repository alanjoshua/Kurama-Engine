package Kurama.ComponentSystem.components.constraints;

import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.components.Rectangle;
import Kurama.Math.Vector;
import Kurama.game.Game;

import java.util.ArrayList;
import java.util.List;

public class ConstraintComponent extends Rectangle {

    public List<Boundary> boundaries = new ArrayList<>();

    public ConstraintComponent(Game game, Component parent, Vector radii, String identifier) {
        super(game, parent, radii, identifier);
    }

    public ConstraintComponent(Game game, Component parent, String identifier) {
        super(game, parent, identifier);
    }

    public ConstraintComponent addBoundary(Boundary b) {
        boundaries.add(b);
        children.add(b);
        return this;
    }

}
