package Kurama.ComponentSystem.components.constraintGUI.RigidBodySystem;

import Kurama.ComponentSystem.components.constraintGUI.BoundInteractionMessage;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;
import Kurama.ComponentSystem.components.constraintGUI.Interactor;
import Kurama.Math.Vector;

public class RigidBoundaryInteractor implements Interactor {
    @Override
    public boolean interact(BoundInteractionMessage info, Boundary boundary, Boundary parentBoundary, int relativePos) {

        if(!(info instanceof RigidBoundaryMessage) || ((info instanceof RigidBoundaryMessage) && ((RigidBoundaryMessage) info).shouldValidify)) {
            if(!boundary.isValidInteraction(info)) {
                return false;
            }
            boundary.resetParams();
        }

        var newInfo = new RigidBoundaryMessage(info);
        newInfo.shouldValidify = false;
        newInfo.parent = parentBoundary;

        if(boundary.boundaryOrient == Boundary.BoundaryOrient.Vertical) {

            if(parentBoundary == null) {
                boundary.pos = boundary.pos.add(new Vector(newInfo.deltaMoveX,0, 0));
                newInfo.parentMoveDir = 0;
            }
            else {
                if(newInfo.parentMoveDir == 0) {
                    boundary.pos = boundary.pos.add(new Vector(newInfo.deltaMoveX,0, 0));
                }
                else {
                    boundary.pos = boundary.pos.add(new Vector(0, newInfo.deltaMoveY, 0));
                }
            }

            boundary.alreadyUpdated = true; // inverted to save some processing time as this variable would already be set by the recursive verification process

            boundary.negativeAttachments.forEach(b -> {
                if(!b.alreadyUpdated) { // inverted to save some processing time as this variable would already be set by the recursive verification process
                    b.interact(newInfo, boundary, -1);
                }
            });
            boundary.positiveAttachments.forEach(b -> {
                if(!b.alreadyUpdated)
                    b.interact(newInfo, boundary, 1);
            });
            return true;
        }
        else { // Horizontal boundary
            if(parentBoundary == null) {
                boundary.pos = boundary.pos.add(new Vector(0, newInfo.deltaMoveY, 0));
                newInfo.parentMoveDir = 1;
            }
            else {
                if(newInfo.parentMoveDir == 0) {
                    boundary.pos = boundary.pos.add(new Vector(newInfo.deltaMoveX,0, 0));
                }
                else {
                    boundary.pos = boundary.pos.add(new Vector(0,  newInfo.deltaMoveY, 0));
                }
            }

            boundary.alreadyUpdated = true;

            boundary.negativeAttachments.forEach(b -> {
                if(!b.alreadyUpdated)
                    b.interact(newInfo, boundary, -1);
            });
            boundary.positiveAttachments.forEach(b -> {
                if(!b.alreadyUpdated)
                    b.interact(newInfo, boundary, 1);
            });
            return true;
        }

    }
}
