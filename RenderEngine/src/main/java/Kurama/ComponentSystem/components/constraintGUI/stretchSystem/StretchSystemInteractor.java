package Kurama.ComponentSystem.components.constraintGUI.stretchSystem;

import Kurama.ComponentSystem.components.constraintGUI.BoundInteractionMessage;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;
import Kurama.ComponentSystem.components.constraintGUI.Interactor;
import Kurama.Math.Vector;
import Kurama.utils.Logger;

public class StretchSystemInteractor implements Interactor {

//    public float minDimThresh = 30;
    public StretchSystemInteractor() {}
//    public StretchSystemInteractor(float minDimThresh) {this.minDimThresh = minDimThresh;}

    @Override
    public boolean interact(BoundInteractionMessage info, Boundary boundary, Boundary parentBoundary, int relativePos) {

        boolean areChildInteractionsValid = true;

        boundary.alreadyVisited = true;

        // vertical being moved either by user
        if(boundary.boundaryOrient == Boundary.BoundaryOrient.Vertical && info.deltaMoveX!=0) {

            boundary.updatedPos = boundary.getPos().add(new Vector(info.deltaMoveX, 0, 0));
            boundary.shouldUpdatePos = true;

            var newInfo = new StretchMessage(info, boundary);

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

            var newInfo = new StretchMessage(info, boundary);

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

            var newInfo = new StretchMessage(info, boundary);

            boundary.updatedWidth = boundary.getWidth() + (-relativePos * info.deltaMoveX);
            boundary.updatedPos = boundary.getPos().add(new Vector(info.deltaMoveX / 2f, 0, 0));

            boundary.shouldUpdateWidth = true;
            boundary.shouldUpdatePos = true;

            if(!recalculatePosDim_horBound(boundary, parentBoundary, info.deltaMoveY, relativePos, newInfo)) return false;

        }

        // most likely being stretched by the stretch system or the rigid system
        else if(boundary.boundaryOrient == Boundary.BoundaryOrient.Vertical && info.deltaMoveY!=0) {

            var newInfo = new StretchMessage(info, boundary);

            boundary.updatedHeight = boundary.getHeight() + relativePos * info.deltaMoveY;
            boundary.updatedPos = boundary.getPos().add(new Vector(0, info.deltaMoveY / 2f, 0));

            boundary.shouldUpdateHeight = true;
            boundary.shouldUpdatePos = true;

            if(!recalculatePosDim_vertBound(boundary, parentBoundary, info.deltaMoveY, relativePos, newInfo)) return false;
        }

        return true;
    }

    public boolean recalculatePosDim_vertBound(Boundary boundary, Boundary parentBoundary, float deltaMoveY, int relativePos, StretchMessage message) {

        if(boundary.negativeAttachments.size() + boundary.positiveAttachments.size() < 2) {
            return true;
        }

        float upperBound = Float.POSITIVE_INFINITY;
        float lowerBound = Float.NEGATIVE_INFINITY;

        for(var b: boundary.positiveAttachments) {

            var posOfBoundBeingComparedTo = b.shouldUpdatePos?b.updatedPos: b.getPos();
            var heightOfBoundBeingComparedTo = (b.shouldUpdateHeight?b.updatedHeight: b.getHeight())/2f;

            if(b.negativeAttachments.contains(boundary)) {
                var tempUpperBound = posOfBoundBeingComparedTo.get(1) - heightOfBoundBeingComparedTo;
                upperBound = (tempUpperBound < upperBound) ? tempUpperBound : upperBound;
            }
            else {
                var tempLowerBound = posOfBoundBeingComparedTo.get(1) + heightOfBoundBeingComparedTo;
                lowerBound = (tempLowerBound > lowerBound) ? tempLowerBound : lowerBound;
            }
        }

        for(var b: boundary.negativeAttachments) {

            var posOfBoundBeingComparedTo = b.shouldUpdatePos?b.updatedPos: b.getPos();
            var heightOfBoundBeingComparedTo = (b.shouldUpdateHeight?b.updatedHeight: b.getHeight())/2f;

            if(b.negativeAttachments.contains(boundary)) {
                var tempUpperBound = posOfBoundBeingComparedTo.get(1) - heightOfBoundBeingComparedTo;
                upperBound = (tempUpperBound < upperBound) ? tempUpperBound : upperBound;
            }
            else {
                var tempLowerBound = posOfBoundBeingComparedTo.get(1) + heightOfBoundBeingComparedTo;
                lowerBound = (tempLowerBound > lowerBound) ? tempLowerBound : lowerBound;
            }

        }

        boundary.updatedHeight = lowerBound - upperBound;
        boundary.updatedPos = new Vector(boundary.updatedPos.geti(0), upperBound+(boundary.updatedHeight/2f), boundary.updatedPos.geti(2));

        if(boundary.updatedHeight < boundary.minHeight) {
//            return false;
            Logger.log(boundary.identifier + " :fixing negative height");
            if(!fixNegativeHeight_vertical(boundary, parentBoundary, relativePos, message)) return false;
        }
        return true;

    }

    public boolean recalculatePosDim_horBound(Boundary boundary, Boundary parentBoundary, float deltaMoveX, int relativePos, StretchMessage message) {

        float leftBound = Float.POSITIVE_INFINITY;
        float rightBound = Float.NEGATIVE_INFINITY;

        for(var b: boundary.positiveAttachments) {

            var posOfBoundBeingComparedTo = b.shouldUpdatePos?b.updatedPos: b.getPos();
            var widthOfBoundBeingComparedTo = (b.shouldUpdateWidth?b.updatedWidth: b.getWidth())/2f;

            if(b.positiveAttachments.contains(boundary)) {
                var tempLeftBound = posOfBoundBeingComparedTo.get(0) - widthOfBoundBeingComparedTo;
                leftBound = (tempLeftBound < leftBound) ? tempLeftBound:leftBound;
            }
            else {
                var tempRightBound = posOfBoundBeingComparedTo.get(0) + widthOfBoundBeingComparedTo;
                rightBound = (tempRightBound > rightBound) ? tempRightBound:rightBound;
            }

        }

        for(var b: boundary.negativeAttachments) {

            var posOfBoundBeingComparedTo = b.shouldUpdatePos?b.updatedPos: b.getPos();
            var widthOfBoundBeingComparedTo = (b.shouldUpdateWidth?b.updatedWidth: b.getWidth())/2f;

            if(b.positiveAttachments.contains(boundary)) {
                var tempLeftBound = posOfBoundBeingComparedTo.get(0) - widthOfBoundBeingComparedTo;
                leftBound = (tempLeftBound < leftBound) ? tempLeftBound:leftBound;
            }
            else {
                var tempRightBound = posOfBoundBeingComparedTo.get(0) + widthOfBoundBeingComparedTo;
                rightBound = (tempRightBound > rightBound) ? tempRightBound:rightBound;
            }

        }

        boundary.updatedWidth = rightBound - leftBound;
        boundary.updatedPos = new Vector(leftBound+(boundary.updatedWidth/2f), boundary.updatedPos.geti(1), boundary.updatedPos.geti(2));

        Logger.log("updated width of "+ boundary.identifier + ": "+ boundary.updatedWidth);


        if(boundary.updatedWidth < boundary.minWidth) {
            Logger.logError(boundary.identifier + " width less than min, so trying to move connected bound");
//            return false;
            Logger.log(boundary.identifier + " :fixing negative width");
            if(!fixNegativeHeight_horizontal(boundary, parentBoundary, relativePos, message)) return false;
        }

        return true;

    }

    // Checks to see whether the height is lower than a certain threshold, or negative (have shrunk and stretched to the other side)
    // Fixes it by stretching the connected boundaries

    // if relative pos is positive, then this vertical bound is being moved by a horizontal boundary from the top, so we now need to move a H-bound at the bottom
    public boolean fixNegativeHeight_vertical(Boundary current, Boundary parent, int relativeParentPos, StretchMessage message) {

        float dy = current.minHeight - current.updatedHeight;

        for(var b: current.positiveAttachments) {
            if(b == parent) continue;

            // original bound being moved down from top
            if(relativeParentPos < 0) {
                if(b.positiveAttachments.contains(current)) {
                    if(!b.interact(new StretchMessage(0, -dy, null, message.shouldOverrideWithinWindowCheck), null, -1)) return false;
                }
            }
            // original bound being moved up from below
            else {
                if(b.negativeAttachments.contains(current)) {
                    if(!b.interact(new StretchMessage(0, dy, null, message.shouldOverrideWithinWindowCheck), null, -1)) return false;
                }
            }
        }

        for(var b: current.negativeAttachments) {
            if(b == parent) continue;

            // original bound being moved down from top
            if(relativeParentPos < 0) {
                if(b.positiveAttachments.contains(current)) {
                    if(!b.interact(new StretchMessage(0, -dy, null, message.shouldOverrideWithinWindowCheck), null, -1)) return false;
                }
            }
            // original bound being moved up from below
            else {
                if(b.negativeAttachments.contains(current)) {
                    if(!b.interact(new StretchMessage(0, dy, null, message.shouldOverrideWithinWindowCheck), null, -1)) return false;
                }
            }
        }

        return true;
    }

    // Checks to see whether the width is lower than a certain threshold, or negative (have shrunk and stretched to the other side)
    // Fixes it by stretching the connected boundaries

    public boolean fixNegativeHeight_horizontal(Boundary current, Boundary parent, int relativeParentPos, StretchMessage message) {

        float dx = current.minWidth - current.updatedWidth;
        Logger.log(current.identifier + ": inside fix negative horizontal dx="+dx);

        for(var b: current.positiveAttachments) {
            if(b == parent) continue;

            // original bound being moved left from right
            if(relativeParentPos < 0) {
                if(b.positiveAttachments.contains(current)) {
                    Logger.logError(current.identifier + " parent=" + parent.identifier + ": fixing hor negative by moving "+ b.identifier);
                    if(!b.interact(new StretchMessage(-dx, 0, null, message.shouldOverrideWithinWindowCheck), null, -1)) return false;

                }
            }
            // original bound being moved up from below
            else {
                if(b.negativeAttachments.contains(current)) {
                    Logger.logError(current.identifier + " parent=" + parent.identifier + ": fixing hor negative by moving "+ b.identifier);
                    if(!b.interact(new StretchMessage(dx, 0, null, message.shouldOverrideWithinWindowCheck), null, -1)) return false;
                }
            }
        }

        for(var b: current.negativeAttachments) {
            if(b == parent) continue;

            // original bound being moved down from top
            if(relativeParentPos < 0) {
                if(b.positiveAttachments.contains(current)) {
                    Logger.logError(current.identifier + " parent=" + parent.identifier + ": fixing hor negative by moving "+ b.identifier);
                    if(!b.interact(new StretchMessage(-dx, 0, null, message.shouldOverrideWithinWindowCheck), null, -1)) return false;
                }
            }
            // original bound being moved up from below
            else {
                if(b.negativeAttachments.contains(current)) {
                    Logger.logError(current.identifier + " parent=" + parent.identifier + ": fixing hor negative by moving "+ b.identifier);
                    if(!b.interact(new StretchMessage(dx, 0, null, message.shouldOverrideWithinWindowCheck), null, -1)) return false;
                }
            }
        }

        return true;
    }

}
