package Kurama.ComponentSystem.components.constraintGUI.RigidBodySystem;

import Kurama.ComponentSystem.components.constraintGUI.BoundMoveDataPack;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;
import Kurama.ComponentSystem.components.constraintGUI.interactionConstraints.InteractionConstraint;

public class RecursiveValidifierConstraint implements InteractionConstraint {

    @Override
    public boolean isValid(Boundary boundary, BoundMoveDataPack info) {
        boundary.alreadyUpdated = true;

        for(var b: boundary.positiveAttachments) {
            if(!b.alreadyUpdated) {
                if (!b.isValidInteraction(info)) {
                    return false;
                }
            }
        }
        for(var b: boundary.negativeAttachments) {
            if(!b.alreadyUpdated) {
                if (!b.isValidInteraction(info)) {
                    return false;
                }
            }
        }

        return true;
    }

}
