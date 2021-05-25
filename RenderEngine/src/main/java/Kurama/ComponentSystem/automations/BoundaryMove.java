package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;
import Kurama.ComponentSystem.components.constraintGUI.VerticalBoundary;
import Kurama.inputs.Input;

// This is intended to be run only as an isOnClickDragged automation
public class BoundaryMove implements Automation {

    private Boundary attachedBoundary;
    private int boundaryType = 0; // 0 = H, 1 = V

    public BoundaryMove(Boundary attachedBoundary) {
        this.attachedBoundary = attachedBoundary;
        if(attachedBoundary instanceof VerticalBoundary) {
            boundaryType = 1;
        }
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        // H-boundary, so can only move vertically
        float deltaMove = 0;
        if(boundaryType == 0) {
            deltaMove = input.mouseDy;
        }
        // V-boundary, so can only move horizontally
        else {
            deltaMove = input.mouseDx;
        }
        attachedBoundary.shouldMove(deltaMove);
    }

}
