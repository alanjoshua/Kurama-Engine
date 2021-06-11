package Kurama.ComponentSystem.components.constraintGUI.stretchSystem;

import Kurama.ComponentSystem.components.constraintGUI.BoundInteractionMessage;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;
import Kurama.ComponentSystem.components.constraintGUI.ConstraintVerificationData;
import Kurama.ComponentSystem.components.constraintGUI.interactionValidifiers.InteractionValidifier;

public class StretchValidifier_Vertical implements InteractionValidifier {

    // Assumes boundary is vertical

    @Override
    public boolean isValid(Boundary boundary, BoundInteractionMessage info, ConstraintVerificationData verificationData) {

        int dHeight = 0;
        int dy = 0;

        if(boundary.shouldUpdateHeight) {

            dHeight = (int) (boundary.updatedHeight - boundary.getHeight());
            dy = boundary.updatedPos.geti(1) - boundary.getPos().geti(1);

            // Moving downwards while shrinking
            if(dy > 0 && dHeight < 0) {
                for(var child: boundary.positiveAttachments) {

                }
            }


        }

        return true;
    }

}
