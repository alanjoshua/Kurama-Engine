package Kurama.ComponentSystem.components.constraintGUI.interactionValidifiers;

import Kurama.ComponentSystem.components.constraintGUI.BoundInteractionMessage;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;
import Kurama.ComponentSystem.components.constraintGUI.ConstraintVerificationData;
import Kurama.ComponentSystem.components.constraintGUI.stretchSystem.StretchMessage;
import Kurama.utils.Logger;

public class EnsureWithinWindow implements InteractionValidifier {
    @Override
    public boolean isValid(Boundary boundary, BoundInteractionMessage info, ConstraintVerificationData verificationData) {

        if(info instanceof StretchMessage && ((StretchMessage) info).shouldOverrideWithinWindowCheck) {
            Logger.logError(boundary.identifier + " overriding");
            return true; // Used when manually resizing
        }

        if(boundary.boundaryOrient == Boundary.BoundaryOrient.Vertical) {
            if ((verificationData.pos.get(0) - verificationData.width / 2f) < -boundary.parent.getWidth() / 2f) {

                float dx = (verificationData.pos.get(0) - verificationData.width / 2f) +(boundary.parent.getWidth() / 2f);

                Logger.logError(boundary.identifier + " not inside parent towards the left side. dx=" + dx + " Returning false...");
                Logger.logError("parent width: "+boundary.parent.getWidth());

                return false;

//                if (!boundary.initialiseInteraction(-dx, 0)) return false;
            }

            if ((verificationData.pos.get(0) + verificationData.width / 2f) > boundary.parent.getWidth() / 2f) {

                float dx = (verificationData.pos.get(0) + verificationData.width / 2f) - (boundary.parent.getWidth() / 2f);

                Logger.logError(boundary.identifier + " not inside parent towards the right side. dx=" + dx + " . Returning false...");

                return false;

//                if (!boundary.initialiseInteraction(-dx, 0)) return false;
            }
        }

        else {
            if ((verificationData.pos.get(1) - verificationData.height / 2f) < -boundary.parent.getHeight() / 2f) {

                float dy = (verificationData.pos.get(1) - verificationData.height / 2f) - (boundary.parent.getHeight() / 2f);

                Logger.logError(boundary.identifier + " not inside parent towards the top. dy=" + dy + " Returning false...");
                Logger.logError("parent height: "+boundary.parent.getHeight());

                return false;

//                if (!boundary.initialiseInteraction(0, -dy)) return false;
            }

            if ((verificationData.pos.get(1) + verificationData.height / 2f) > boundary.parent.getHeight() / 2f) {

                float dy = (verificationData.pos.get(1) + verificationData.height / 2f) - (boundary.parent.getHeight() / 2f);

                Logger.logError(boundary.identifier + " not inside parent towards the bottom. dy=" + dy + " Returning false...");
                Logger.logError("parent height: "+boundary.parent.getHeight());

                return false;

//                if (!boundary.initialiseInteraction(0, -dy)) return false;
            }
        }

        return true;
    }
}
