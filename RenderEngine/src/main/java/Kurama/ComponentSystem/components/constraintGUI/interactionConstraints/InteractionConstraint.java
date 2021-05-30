package Kurama.ComponentSystem.components.constraintGUI.interactionConstraints;

import Kurama.ComponentSystem.components.constraintGUI.BoundInteractionMessage;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;

public interface InteractionConstraint {

    public abstract boolean isValid(Boundary boundary, BoundInteractionMessage info);

}
