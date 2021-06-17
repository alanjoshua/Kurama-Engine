package Kurama.ComponentSystem.components.constraintGUI.stretchSystem;

import Kurama.ComponentSystem.components.constraintGUI.BoundInteractionMessage;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;
import Kurama.ComponentSystem.components.constraintGUI.ConstraintVerificationData;
import Kurama.ComponentSystem.components.constraintGUI.interactionValidifiers.InteractionValidifier;
import Kurama.utils.Logger;

public class EnsureDimensionsValid implements InteractionValidifier {
    @Override
    public boolean isValid(Boundary boundary, BoundInteractionMessage info, ConstraintVerificationData verificationData) {

        if(verificationData.width < boundary.minWidth) {
            Logger.logError(boundary.identifier + " width too small");
            return false;
        }

        if(verificationData.width > boundary.maxWidth) {
            Logger.logError(boundary.identifier + " width too large");
            return false;
        }

        if(verificationData.height < boundary.minHeight) {
            Logger.logError(boundary.identifier + " height too small");
            return false;
        }

        if(verificationData.height > boundary.maxHeight) {
            Logger.logError(boundary.identifier + " height too large");
            return false;
        }

        return true;
    }
}
