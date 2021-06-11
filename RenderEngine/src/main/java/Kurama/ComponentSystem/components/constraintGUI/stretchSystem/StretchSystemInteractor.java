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

            boundary.updatedPos = boundary.getPos().add(new Vector(info.deltaMoveX, 0, 0));
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

            boundary.updatedPos = boundary.getPos().add(new Vector(0, info.deltaMoveY, 0));
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


            boundary.updatedWidth = boundary.getWidth() + (-relativePos * info.deltaMoveX);
            boundary.updatedPos = boundary.getPos().add(new Vector(info.deltaMoveX / 2f, 0, 0));

            boundary.shouldUpdateWidth = true;
            boundary.shouldUpdatePos = true;

            rectifyMovement_horizontal(boundary, parentBoundary, info.deltaMoveY, relativePos);

        }

        // most likely being stretched by the stretch system or the rigid system
        else if(boundary.boundaryOrient == Boundary.BoundaryOrient.Vertical && info.deltaMoveY!=0) {

            // Add check here to see whether it should remain fixed or not

            boundary.updatedHeight = boundary.getHeight() + relativePos * info.deltaMoveY;
            boundary.updatedPos = boundary.getPos().add(new Vector(0, info.deltaMoveY / 2f, 0));

            boundary.shouldUpdateHeight = true;
            boundary.shouldUpdatePos = true;

            rectifyMovement_vertical(boundary, parentBoundary, info.deltaMoveY, relativePos);
        }

        return true;
    }

    public boolean rectifyMovement_vertical(Boundary boundary, Boundary parentBoundary, float deltaMoveY, int relativePos) {

        float upperBound = Float.POSITIVE_INFINITY;
        float lowerBound = Float.NEGATIVE_INFINITY;

        for(var b: boundary.positiveAttachments) {

            var posOfBoundBeingComparedTo = b.shouldUpdatePos?b.updatedPos: b.getPos();
            var heightOfBoundBeingComparedTo = (b.shouldUpdateHeight?b.updatedHeight: b.getHeight())/2f;

            var tempUpperBound = posOfBoundBeingComparedTo.get(1) - heightOfBoundBeingComparedTo;
            var tempLowerBound = posOfBoundBeingComparedTo.get(1) + heightOfBoundBeingComparedTo;

            upperBound = (tempUpperBound < upperBound) ? tempUpperBound:upperBound;
            lowerBound = (tempLowerBound > lowerBound) ? tempLowerBound:lowerBound;
        }

        for(var b: boundary.negativeAttachments) {

            var posOfBoundBeingComparedTo = b.shouldUpdatePos?b.updatedPos: b.getPos();
            var heightOfBoundBeingComparedTo = (b.shouldUpdateHeight?b.updatedHeight: b.getHeight())/2f;

            var tempUpperBound = posOfBoundBeingComparedTo.get(1) - heightOfBoundBeingComparedTo;
            var tempLowerBound = posOfBoundBeingComparedTo.get(1) + heightOfBoundBeingComparedTo;

            upperBound = (tempUpperBound < upperBound) ? tempUpperBound:upperBound;
            lowerBound = (tempLowerBound > lowerBound) ? tempLowerBound:lowerBound;
        }

        boundary.updatedHeight = lowerBound - upperBound;
        boundary.updatedPos = new Vector(boundary.updatedPos.geti(0), upperBound+(boundary.updatedHeight/2f), boundary.updatedPos.geti(2));

        return true;

    }

    public boolean rectifyMovement_horizontal(Boundary boundary, Boundary parentBoundary, float deltaMoveX, int relativePos) {

        float leftBound = Float.POSITIVE_INFINITY;
        float rightBound = Float.NEGATIVE_INFINITY;

        for(var b: boundary.positiveAttachments) {

            var posOfBoundBeingComparedTo = b.shouldUpdatePos?b.updatedPos: b.getPos();
            var widthOfBoundBeingComparedTo = (b.shouldUpdateWidth?b.updatedWidth: b.getWidth())/2f;

            var tempLeftBound = posOfBoundBeingComparedTo.get(0) - widthOfBoundBeingComparedTo;
            var tempRightBound = posOfBoundBeingComparedTo.get(0) + widthOfBoundBeingComparedTo;

            leftBound = (tempLeftBound < leftBound) ? tempLeftBound:leftBound;
            rightBound = (tempRightBound > rightBound) ? tempRightBound:rightBound;
        }

        for(var b: boundary.negativeAttachments) {

            var posOfBoundBeingComparedTo = b.shouldUpdatePos?b.updatedPos: b.getPos();
            var widthOfBoundBeingComparedTo = (b.shouldUpdateWidth?b.updatedWidth: b.getWidth())/2f;

            var tempLeftBound = posOfBoundBeingComparedTo.get(0) - widthOfBoundBeingComparedTo;
            var tempRightBound = posOfBoundBeingComparedTo.get(0) + widthOfBoundBeingComparedTo;

            leftBound = (tempLeftBound < leftBound) ? tempLeftBound:leftBound;
            rightBound = (tempRightBound > rightBound) ? tempRightBound:rightBound;
        }

        boundary.updatedWidth = rightBound - leftBound;
        boundary.updatedPos = new Vector(leftBound+(boundary.updatedWidth/2f), boundary.updatedPos.geti(1), boundary.updatedPos.geti(2));

        return true;

    }

}
