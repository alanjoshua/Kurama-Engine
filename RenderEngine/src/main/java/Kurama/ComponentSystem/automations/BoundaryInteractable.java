package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;
import Kurama.inputs.Input;

// This is intended to be run only as an isOnClickDragged automation
public class BoundaryInteractable implements Automation {

    private Boundary attachedBoundary;

    public BoundaryInteractable(Boundary attachedBoundary) {
        this.attachedBoundary = attachedBoundary;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        attachedBoundary.initialiseInteraction(input.mouseDx, input.mouseDy);
    }

}
