package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;
import Kurama.inputs.Input;

// This is intended to be run only as an isOnClickDragged automation
public class BoundaryMove implements Automation {

    private Boundary attachedBoundary;

    public BoundaryMove(Boundary attachedBoundary) {
        this.attachedBoundary = attachedBoundary;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        // H-boundary, so can only move vertically
        float deltaMove;
        if(attachedBoundary.boundaryOrient == Boundary.BoundaryOrient.Horizontal) {
            deltaMove = input.mouseDy;
        }
        // V-boundary, so can only move horizontally
        else {
            deltaMove = input.mouseDx;
        }
        attachedBoundary.initialiseInteraction(deltaMove);
    }

}
