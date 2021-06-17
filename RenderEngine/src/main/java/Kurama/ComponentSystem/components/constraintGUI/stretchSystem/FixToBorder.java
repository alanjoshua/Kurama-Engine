package Kurama.ComponentSystem.components.constraintGUI.stretchSystem;

import Kurama.ComponentSystem.components.constraintGUI.BoundInteractionMessage;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;
import Kurama.ComponentSystem.components.constraintGUI.ConstraintVerificationData;
import Kurama.ComponentSystem.components.constraintGUI.interactionValidifiers.InteractionValidifier;
import Kurama.utils.Logger;

public class FixToBorder implements InteractionValidifier {

    public enum AttachPoint {left, right, top, bottom}
    public AttachPoint attachPoint;

    public FixToBorder(AttachPoint ap) {
        attachPoint = ap;
    }

    @Override
    public boolean isValid(Boundary boundary, BoundInteractionMessage info, ConstraintVerificationData verificationData) {

        if(info instanceof StretchMessage && ((StretchMessage) info).shouldOverrideWithinWindowCheck) {
            Logger.logError(boundary.identifier + " overriding");
            return true; // Used when manually resizing
        }

        if(attachPoint == AttachPoint.left) {

            if ((int)(verificationData.pos.get(0) - verificationData.width / 2f) != (int)(-boundary.parent.getWidth() / 2f)) {
                Logger.logError(boundary.identifier + " not attached to left");
                return false;
            }

        }
        else if(attachPoint == AttachPoint.right) {

            if ((int)(verificationData.pos.get(0) + verificationData.width / 2f) > (int)(boundary.parent.getWidth() / 2f)) {
                Logger.logError(boundary.identifier + " not attached to right");
                return false;
            }

        }
        else if(attachPoint == AttachPoint.top) {
            if ((int)(verificationData.pos.get(1) - verificationData.height / 2f) < (int)(-boundary.parent.getHeight() / 2f)) {
                Logger.logError(boundary.identifier + " not attached to top");
                return false;
            }
        }
        // Bottom part
        else {
            if ((int)(verificationData.pos.get(1) + verificationData.height / 2f) > (int)(boundary.parent.getHeight() / 2f)) {
                Logger.logError(boundary.identifier + " not attached to bottom");
                return false;
            }
        }

        return true;
    }
}
