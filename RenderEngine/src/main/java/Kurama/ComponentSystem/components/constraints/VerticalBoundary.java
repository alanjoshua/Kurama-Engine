package Kurama.ComponentSystem.components.constraints;

import Kurama.ComponentSystem.automations.OnlyVerticalCompDrag;
import Kurama.ComponentSystem.components.Component;
import Kurama.game.Game;

public class VerticalBoundary extends Boundary {

    public VerticalBoundary(Game game, Component parent, String identifier, boolean isDraggable) {
        super(game, parent, identifier);
        width = 10;
        if(isDraggable) addOnClickDraggedAction(new OnlyVerticalCompDrag());
    }

    public VerticalBoundary(Game game, Component parent, String identifier, int borderSize, boolean isDraggable) {
        super(game, parent, identifier);
        width = borderSize;
        if(isDraggable) addOnClickDraggedAction(new OnlyVerticalCompDrag());
    }

}
