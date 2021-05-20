package Kurama.ComponentSystem.components.constraints;

import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.components.Rectangle;
import Kurama.Math.Vector;
import Kurama.game.Game;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Boundary extends Rectangle {

    public Map<BoundaryType, List<Component>> attachedComponents = new HashMap<>();

    public Boundary(Game game, Component parent, boolean isDraggable, String identifier) {
        super(game, parent, identifier);
        color = new Vector(0,0,0,1);
    }

    public abstract Boundary bindComponent(Component comp, BoundaryType boundaryType);
    public abstract void updateBoundComponents();

}
