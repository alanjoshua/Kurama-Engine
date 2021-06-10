package Kurama.ComponentSystem.components.constraintGUI.interactionValidifiers;

import Kurama.ComponentSystem.components.constraintGUI.BoundInteractionMessage;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;
import Kurama.ComponentSystem.components.constraintGUI.ConstraintVerificationData;

public class EnsureNoNearZeroDimensions implements InteractionValidifier {

    float thresh = 10;

    public EnsureNoNearZeroDimensions(float thresh) {
        this.thresh = thresh;
    }
    public EnsureNoNearZeroDimensions() { }


    @Override
    public boolean isValid(Boundary boundary, BoundInteractionMessage info, ConstraintVerificationData verificationData) {

        if(boundary.boundaryOrient == Boundary.BoundaryOrient.Vertical) {
            var deltaChange = verificationData.height - boundary.height;

            if (deltaChange < 0 && verificationData.height < thresh) {
                return false;
            }

        }
        if(boundary.boundaryOrient == Boundary.BoundaryOrient.Horizontal) {

            var deltaChange = verificationData.width - boundary.width;
            if(deltaChange < 0 && verificationData.width < thresh) {
                return false;
            }
        }

        return true;
    }
}
