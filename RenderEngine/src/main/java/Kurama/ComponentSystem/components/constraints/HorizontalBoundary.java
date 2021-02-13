package Kurama.ComponentSystem.components.constraints;

import Kurama.ComponentSystem.automations.OnlyHorizontalCompDrag;
import Kurama.ComponentSystem.components.Component;
import Kurama.game.Game;

public class HorizontalBoundary extends Boundary {

    public HorizontalBoundary(Game game, Component parent, String identifier, boolean isDraggable) {
        super(game, parent, identifier);
        height = 10;
        if(isDraggable) addOnClickDraggedAction(new OnlyHorizontalCompDrag());
    }

    public HorizontalBoundary(Game game, Component parent, String identifier, int borderSize, boolean isDraggable) {
        super(game, parent, identifier);
        height = borderSize;
        if(isDraggable) addOnClickDraggedAction(new OnlyHorizontalCompDrag());
    }

}
