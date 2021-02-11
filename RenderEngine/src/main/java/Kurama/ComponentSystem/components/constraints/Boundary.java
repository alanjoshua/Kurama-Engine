package Kurama.ComponentSystem.components.constraints;

import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.components.Rectangle;
import Kurama.Math.Vector;
import Kurama.game.Game;

import java.util.ArrayList;
import java.util.List;

public abstract class Boundary extends Rectangle {

    public List<Component> attachedComponents = new ArrayList<>();

    public Boundary(Game game, Component parent, String identifier) {
        super(game, parent, identifier);
        color = new Vector(0,0,0,1);
    }
}
