package Kurama.ComponentSystem.components.constraints;

import Kurama.ComponentSystem.automations.OnlyHorizontalCompDrag;
import Kurama.ComponentSystem.components.Component;
import Kurama.game.Game;
import Kurama.utils.Logger;

import java.util.ArrayList;

public class HorizontalBoundary extends Boundary {

    public HorizontalBoundary(Game game, Component parent, String identifier, boolean isDraggable) {
        super(game, parent, isDraggable, identifier);
        height = 10;
        if(isDraggable) {
            addOnClickDraggedAction(new OnlyHorizontalCompDrag());
        }
    }

    public HorizontalBoundary(Game game, Component parent, String identifier, int borderSize, boolean isDraggable) {
        super(game, parent, isDraggable, identifier);
        height = borderSize;
        if(isDraggable) {
            addOnClickDraggedAction(new OnlyHorizontalCompDrag());
            addOnClickDraggedAction((comp, input, timeDelta) -> updateBoundComponents());
        }
    }

    @Override
    public Boundary bindComponent(Component comp, BoundaryType boundaryType) {

        if(boundaryType != BoundaryType.TOP && boundaryType != BoundaryType.BOTTOM) {
            Logger.logError("Illegal argument provided to bindComponent. Returning without doing anything");
            return this;
        }
        attachedComponents.putIfAbsent(boundaryType, new ArrayList<>());
        attachedComponents.get(boundaryType).add(comp);
        return this;
    }

    @Override
    public void updateBoundComponents() {

        if(attachedComponents.get(BoundaryType.TOP) != null) {
            for (var comp : attachedComponents.get(BoundaryType.TOP)) {
                comp.pos.setDataElement(1, this.pos.geti(1)+comp.height/2);
            }
        }

        if(attachedComponents.get(BoundaryType.BOTTOM) != null) {
            for (var comp : attachedComponents.get(BoundaryType.BOTTOM)) {
                comp.height = (int) ((pos.geti(1) - comp.pos.geti(1)) * 2);
            }
        }
    }
}
