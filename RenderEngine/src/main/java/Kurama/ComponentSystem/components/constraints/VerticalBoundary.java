package Kurama.ComponentSystem.components.constraints;

import Kurama.ComponentSystem.automations.OnlyVerticalCompDrag;
import Kurama.ComponentSystem.components.Component;
import Kurama.game.Game;
import Kurama.utils.Logger;

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

    @Override
    public Boundary bindComponent(Component comp, BoundaryType boundaryType) {
        if(boundaryType != BoundaryType.LEFT || boundaryType != BoundaryType.RIGHT) {
            Logger.logError("Illegal argument provided to bindComponent. Returning without doing anything");
            return this;
        }

        return this;
    }

    @Override
    public void updateBoundComponents() { // Probably should be called only when boundary is moved

        for(var comp: attachedComponents.get(BoundaryType.LEFT)) {
            comp.pos.setDataElement(0, this.pos.get(0));
        }

        for(var comp: attachedComponents.get(BoundaryType.RIGHT)) {
            comp.width = (int) ((pos.get(0) - comp.pos.get(0)) * 2f);
        }

    }
}
