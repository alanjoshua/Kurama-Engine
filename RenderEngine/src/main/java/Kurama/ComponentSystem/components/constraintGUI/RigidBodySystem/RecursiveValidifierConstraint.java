package Kurama.ComponentSystem.components.constraintGUI.RigidBodySystem;

import Kurama.ComponentSystem.components.constraintGUI.BoundInteractionMessage;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;
import Kurama.ComponentSystem.components.constraintGUI.interactionConstraints.InteractionConstraint;

public class RecursiveValidifierConstraint implements InteractionConstraint {

    @Override
    public boolean isValid(Boundary boundary, BoundInteractionMessage info) {

        boundary.alreadyUpdated = true;
        var newMessage = new RigidBoundaryMessage(info);
        newMessage.shouldValidify = false;

        for(var b: boundary.positiveAttachments) {

            if(!b.alreadyUpdated) {
                if (!b.isValidInteraction(newMessage)) {
                    return false;
                }
            }
        }
        for(var b: boundary.negativeAttachments) {
            if(!b.alreadyUpdated) {
                if (!b.isValidInteraction(newMessage)) {
                    return false;
                }
            }
        }

        return true;
    }

}
