package Kurama.ComponentSystem.components.constraintGUI.interactionConstraints;

import Kurama.ComponentSystem.components.constraintGUI.BoundInteractionMessage;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;
import Kurama.ComponentSystem.components.constraintGUI.ConstraintVerificationData;

public interface InteractionConstraint {

    public abstract boolean isValid(Boundary boundary, BoundInteractionMessage info, ConstraintVerificationData verificationData);

}
