package Kurama.ComponentSystem.components.constraintGUI.stretchSystem;

import Kurama.ComponentSystem.components.constraintGUI.BoundInteractionMessage;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;
import Kurama.ComponentSystem.components.constraintGUI.ConstraintVerificationData;
import Kurama.ComponentSystem.components.constraintGUI.interactionConstraints.InteractionConstraint;

public class StretchConstraint implements InteractionConstraint {
    @Override
    public boolean isValid(Boundary boundary, BoundInteractionMessage info, ConstraintVerificationData verificationData) {

        if(boundary.boundaryOrient == Boundary.BoundaryOrient.Vertical) {

        }
        // Horizontal
        else {

            // moving down
            if(info.deltaMoveY >= 0) {

            }
        }

        return true;
    }
}
