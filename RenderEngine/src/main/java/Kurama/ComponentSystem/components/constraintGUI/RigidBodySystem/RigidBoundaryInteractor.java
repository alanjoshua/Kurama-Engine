package Kurama.ComponentSystem.components.constraintGUI.RigidBodySystem;

import Kurama.ComponentSystem.components.constraintGUI.BoundInteractionMessage;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;
import Kurama.ComponentSystem.components.constraintGUI.Interactor;
import Kurama.Math.Vector;

public class RigidBoundaryInteractor implements Interactor {
    @Override
    public boolean interact(BoundInteractionMessage info, Boundary boundary, Boundary parentBoundary, int relativePos) {

//        if(info instanceof StretchMessage) {
//            return false; // Since we know this system cannot stretch, no use in interacting
//        }

//        if(!(info instanceof RigidBoundaryMessage) || ((info instanceof RigidBoundaryMessage) && ((RigidBoundaryMessage) info).shouldValidify)) {
//            if(!boundary.isValidInteraction_pre(info)) {
//                return false;
//            }
//            boundary.resetParams();
//        }

        boolean areChildInteractionsValid = true;

        var newInfo = new RigidBoundaryMessage(info);
//        newInfo.shouldValidify = false;
        newInfo.parent = parentBoundary;
        boundary.alreadyVisited = true;

        if(boundary.boundaryOrient == Boundary.BoundaryOrient.Vertical) {

            if(parentBoundary == null) {
                boundary.updatedPos = boundary.pos.add(new Vector(newInfo.deltaMoveX,0, 0));
                boundary.shouldUpdatePos = true;
                newInfo.parentMoveDir = 0;
            }
            else {
                if(newInfo.parentMoveDir == 0) {
                    boundary.updatedPos = boundary.pos.add(new Vector(newInfo.deltaMoveX,0, 0));
                    boundary.shouldUpdatePos = true;
                }
                else {
                    boundary.updatedPos = boundary.pos.add(new Vector(0, newInfo.deltaMoveY, 0));
                    boundary.shouldUpdatePos = true;
                }
            }

            for(var b: boundary.negativeAttachments) {
                if(!b.alreadyVisited) {
                    areChildInteractionsValid = b.interact(newInfo, boundary, -1);
                    if(!areChildInteractionsValid) return false;
                }
            }
            for(var b: boundary.positiveAttachments) {
                if(!b.alreadyVisited)
                    areChildInteractionsValid = b.interact(newInfo, boundary, 1);
                    if(!areChildInteractionsValid) return false;
            }

            return true;
        }
        else { // Horizontal boundary

            if(parentBoundary == null) {
                boundary.updatedPos = boundary.pos.add(new Vector(0, newInfo.deltaMoveY, 0));
                boundary.shouldUpdatePos = true;
                newInfo.parentMoveDir = 1;
            }
            else {
                if(newInfo.parentMoveDir == 0) {
                    boundary.updatedPos = boundary.pos.add(new Vector(newInfo.deltaMoveX,0, 0));
                    boundary.shouldUpdatePos = true;
                }
                else {
                    boundary.updatedPos = boundary.pos.add(new Vector(0,  newInfo.deltaMoveY, 0));
                    boundary.shouldUpdatePos = true;
                }
            }

            for(var b: boundary.negativeAttachments) {
                if(!b.alreadyVisited) {
                    areChildInteractionsValid = b.interact(newInfo, boundary, -1);
                    if(!areChildInteractionsValid) return false;
                }
            }
            for(var b: boundary.positiveAttachments) {
                if(!b.alreadyVisited)
                    areChildInteractionsValid = b.interact(newInfo, boundary, 1);
                if(!areChildInteractionsValid) return false;
            }
            return true;
        }

    }
}
