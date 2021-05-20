package Kurama.ComponentSystem.components.constraints;

import Kurama.ComponentSystem.automations.VerticalBoundaryDrag;
import Kurama.ComponentSystem.components.Component;
import Kurama.Math.Vector;
import Kurama.game.Game;
import Kurama.utils.Logger;

import java.util.ArrayList;

public class VerticalBoundary extends Boundary {

    public VerticalBoundary(Game game, Component parent, String identifier, boolean isDraggable) {
        super(game, parent, isDraggable, identifier);
        width = 10;
        if(isDraggable) {
            addOnClickDraggedAction(new VerticalBoundaryDrag());
        }
    }

    public VerticalBoundary(Game game, Component parent, String identifier, int borderSize, boolean isDraggable) {
        super(game, parent, isDraggable, identifier);
        width = borderSize;
        if(isDraggable) {
            addOnClickDraggedAction(new VerticalBoundaryDrag());
            addOnClickDraggedAction((comp, input, timeDelta) -> updateBoundComponents());
        }
    }

    @Override
    public Boundary bindComponent(Component comp, BoundaryType boundaryType) {
        if(boundaryType != BoundaryType.LEFT && boundaryType != BoundaryType.RIGHT) {
            Logger.logError("Illegal argument provided to Vertical bindComponent. Returning without doing anything");
            return this;
        }

        attachedComponents.putIfAbsent(boundaryType, new ArrayList<>());
        attachedComponents.get(boundaryType).add(comp);

        return this;
    }

    @Override
    public void updateBoundComponents() { // Probably should be called only when boundary is moved

        if(attachedComponents.get(BoundaryType.LEFT) != null) {
            for (var comp : attachedComponents.get(BoundaryType.LEFT)) {

                if(comp.pos.geti(0) <= (this.pos.geti(0) + this.width)) {

                    int deltaPos = -(comp.pos.geti(0) - comp.width / 2) + (pos.geti(0) + width / 2);
                    comp.pos = comp.pos.add(new Vector(deltaPos, 0, 0));
                    Logger.log("detla pos: " + deltaPos);
                }
//                comp.width -= (deltaPos/2);
            }
        }

        if(attachedComponents.get(BoundaryType.RIGHT) != null) {
            for (var comp : attachedComponents.get(BoundaryType.RIGHT)) {

                if(comp.pos.geti(0) + comp.width >= this.pos.geti(0)) {
//                int newPos = this.pos.geti(0)+comp.width/2;
                    comp.width = (int) ((pos.geti(0) - comp.pos.geti(0)) * 2);
                }
            }
        }

    }

    public void updateComp(Component comp, BoundaryType type) {
        if(type == BoundaryType.LEFT) {

        }

        else if(type == BoundaryType.RIGHT) {

        }
    }

}
