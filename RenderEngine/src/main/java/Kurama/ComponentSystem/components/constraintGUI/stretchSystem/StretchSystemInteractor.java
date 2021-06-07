package Kurama.ComponentSystem.components.constraintGUI.stretchSystem;

import Kurama.ComponentSystem.components.constraintGUI.BoundInteractionMessage;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;
import Kurama.ComponentSystem.components.constraintGUI.Interactor;
import Kurama.Math.Vector;

public class StretchSystemInteractor implements Interactor {
    @Override
    public boolean interact(BoundInteractionMessage info, Boundary boundary, Boundary parentBoundary, int relativePos) {

        boolean areChildInteractionsValid = true;

        boundary.alreadyVisited = true;

        // vertical being moved either by user
        if(boundary.boundaryOrient == Boundary.BoundaryOrient.Vertical && info.deltaMoveX!=0) {

            boundary.updatedPos = boundary.pos.add(new Vector(info.deltaMoveX, 0, 0));
            boundary.shouldUpdatePos = true;

            var newInfo = new StretchMessage(info);

            for(var b: boundary.positiveAttachments) {
                areChildInteractionsValid = b.interact(newInfo, boundary, 1);
                if(!areChildInteractionsValid) return false;
            }
            for(var b: boundary.negativeAttachments) {
                areChildInteractionsValid = b.interact(newInfo, boundary, -1);
                if(!areChildInteractionsValid) return false;
            }
        }

        // Horizontal being moved either by user or rigid system
        else if(boundary.boundaryOrient == Boundary.BoundaryOrient.Horizontal && info.deltaMoveY!=0) {

            boundary.updatedPos = boundary.pos.add(new Vector(0, info.deltaMoveY, 0));
            boundary.shouldUpdatePos = true;

            var newInfo = new StretchMessage(info);

            for(var b: boundary.positiveAttachments) {
                areChildInteractionsValid = b.interact(newInfo, boundary, 1);
                if(!areChildInteractionsValid) return false;
            }
            for(var b: boundary.negativeAttachments) {
                areChildInteractionsValid = b.interact(newInfo, boundary, -1);
                if(!areChildInteractionsValid) return false;
            }
        }

        // most likely being stretched by the stretch system or the rigid system
        else if(boundary.boundaryOrient == Boundary.BoundaryOrient.Horizontal && info.deltaMoveX!=0) {

            // Add check here to see whether it should remain fixed or not

            if(isSecondaryHorizontalBoundMoveValid(boundary, parentBoundary, info.deltaMoveY, relativePos)) {
                boundary.updatedWidth = boundary.width + (-relativePos * info.deltaMoveX);
                boundary.updatedPos = boundary.pos.add(new Vector(info.deltaMoveX / 2f, 0, 0));

                boundary.shouldUpdateWidth = true;
                boundary.shouldUpdatePos = true;
            }
        }

        // most likely being stretched by the stretch system or the rigid system
        else if(boundary.boundaryOrient == Boundary.BoundaryOrient.Vertical && info.deltaMoveY!=0) {

            // Add check here to see whether it should remain fixed or not
            if(isSecondaryVertBoundMoveValid(boundary, parentBoundary, info.deltaMoveY, relativePos)) {
                boundary.updatedHeight = boundary.height + relativePos * info.deltaMoveY;
                boundary.updatedPos = boundary.pos.add(new Vector(0, info.deltaMoveY / 2f, 0));

                boundary.shouldUpdateHeight = true;
                boundary.shouldUpdatePos = true;
            }
        }

        return true;
    }

    public boolean isSecondaryVertBoundMoveValid(Boundary boundary, Boundary parentBoundary, float deltaMoveY, int relativePos) {

        var tentativePos = boundary.pos.add(new Vector(0, deltaMoveY/2f, 0));
        var tentativeHeight = boundary.height + relativePos * deltaMoveY;

        boolean isAnyHBoundLeftHanging = false;

        for(var b: boundary.positiveAttachments) {
            var bPos = b.shouldUpdatePos?b.updatedPos:b.pos;

            if(deltaMoveY >= 0) { // Moving down
                if(bPos.geti(1) < tentativePos.geti(1)) {
                    isAnyHBoundLeftHanging = true;
                    break;
                }
            }
            else {
                if(bPos.geti(1) > tentativePos.geti(1)) {
                    isAnyHBoundLeftHanging = true;
                    break;
                }
            }
        }

        if(!isAnyHBoundLeftHanging) {
            for (var b : boundary.negativeAttachments) {
                var posOfBoundBeingComparedTo = b.shouldUpdatePos ? b.updatedPos : b.pos;

                if (deltaMoveY >= 0) { // Moving down
                    if (posOfBoundBeingComparedTo.geti(1) < tentativePos.geti(1)) {
                        isAnyHBoundLeftHanging = true;
                        break;
                    }
                } else {
                    if (posOfBoundBeingComparedTo.geti(1) > tentativePos.geti(1)) {
                        isAnyHBoundLeftHanging = true;
                        break;
                    }
                }
            }
        }

        return true;

    }

    public boolean isSecondaryHorizontalBoundMoveValid(Boundary boundary, Boundary parentBoundary, float deltaMoveX, int relativePos) {

        var tentativeWidth = boundary.width + (-relativePos * deltaMoveX);
        var tentativePos = boundary.pos.add(new Vector(deltaMoveX/2f, 0, 0));

        boolean isAnyHBoundLeftHanging = false;

        for(var b: boundary.positiveAttachments) {
            if(b == parentBoundary) continue;
            var posOfBoundBeingComparedTo = b.shouldUpdatePos?b.updatedPos:b.pos;

            if(deltaMoveX >= 0) { // Moving down
                if(posOfBoundBeingComparedTo.geti(0) < tentativePos.geti(0)) {
                    isAnyHBoundLeftHanging = true;
                    break;
                }
            }
            else {
                if(posOfBoundBeingComparedTo.geti(0) > tentativePos.geti(0)) {
                    isAnyHBoundLeftHanging = true;
                    break;
                }
            }
        }

        if(!isAnyHBoundLeftHanging) {
            for (var b : boundary.negativeAttachments) {
                if(b == parentBoundary) continue;
                var bPos = b.shouldUpdatePos ? b.updatedPos : b.pos;

                if (deltaMoveX >= 0) { // Moving right
                    if (bPos.geti(0) < tentativePos.geti(0)) {
                        isAnyHBoundLeftHanging = true;
                        break;
                    }
                } else {
                    if (bPos.geti(0) > tentativePos.geti(0)) {
                        isAnyHBoundLeftHanging = true;
                        break;
                    }
                }
            }
        }

        return true;

    }

}
