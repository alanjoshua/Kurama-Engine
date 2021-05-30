package Kurama.ComponentSystem.components.constraintGUI.stretchSystem;

import Kurama.ComponentSystem.components.constraintGUI.BoundInteractionMessage;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;
import Kurama.ComponentSystem.components.constraintGUI.Interactor;
import Kurama.Math.Vector;
import Kurama.utils.Logger;

public class StretchSystemInteractor implements Interactor {
    @Override
    public boolean interact(BoundInteractionMessage info, Boundary boundary, Boundary parentBoundary, int relativePos) {

        boolean verified = false;

        if(!(info instanceof StretchMessage) || ((info instanceof StretchMessage) && ((StretchMessage) info).shouldValidify)) {

            if(!boundary.isValidInteraction(info)) {
                return false;
            }

            for(var b: boundary.positiveAttachments) {
                if(!b.isValidInteraction(info)) {
                    return false;
                }
            }

            for(var b: boundary.negativeAttachments) {
                if(!b.isValidInteraction(info)) {
                    return false;
                }
            }

            verified = true;
        }

        boundary.alreadyUpdated = true;

        // vertical being moved either by user
        if(boundary.boundaryOrient == Boundary.BoundaryOrient.Vertical && info.deltaMoveX!=0) {

            Logger.log("inside stretch vert");

            boundary.pos = boundary.pos.add(new Vector(info.deltaMoveX, 0, 0));

            var newInfo = new StretchMessage(info);
            newInfo.shouldValidify = !verified;

//            var addVec = new Vector(info.deltaMoveX/2f, 0, 0);
            for(var b: boundary.positiveAttachments) {
                b.interact(newInfo, boundary, 1);

//                b.width -= info.deltaMoveX;
//                b.pos = b.pos.add(addVec);
            }
            for(var b: boundary.negativeAttachments) {
                b.interact(newInfo, boundary, -1);
//                b.width += info.deltaMoveX;
//                b.pos = b.pos.add(addVec);
            }
        }

        // Horizontal being moved either by user
        else if(boundary.boundaryOrient == Boundary.BoundaryOrient.Horizontal && info.deltaMoveY!=0) {

            Logger.log("inside stretch hor: "+boundary.identifier);

            boundary.pos = boundary.pos.add(new Vector(0, info.deltaMoveY, 0));

            var newInfo = new StretchMessage(info);
            newInfo.shouldValidify = !verified;

//            var addVec = new Vector(0, info.deltaMoveY/2f, 0);
            for(var b: boundary.positiveAttachments) {
                b.interact(newInfo, boundary, 1);
//                b.height += info.deltaMoveY;
//                b.pos = b.pos.add(addVec);
            }
            for(var b: boundary.negativeAttachments) {
                b.interact(newInfo, boundary, -1);
//                b.height -= info.deltaMoveY;
//                b.pos = b.pos.add(addVec);
            }
        }

        // most likely being stretched by the stretch system or the rigid system
        else if(boundary.boundaryOrient == Boundary.BoundaryOrient.Horizontal && info.deltaMoveX!=0) {

            Logger.log("inside hor x mov");

            var addVec = new Vector(info.deltaMoveX/2f, 0, 0);

            boundary.width += (-relativePos * info.deltaMoveX);
            boundary.pos = boundary.pos.add(addVec);
        }

        // most likely being stretched by the stretch system or the rigid system
        else if(boundary.boundaryOrient == Boundary.BoundaryOrient.Vertical && info.deltaMoveY!=0) {

            Logger.log("inside vert y mov");

            var addVec = new Vector(0, info.deltaMoveY/2f, 0);
            boundary.height += relativePos * info.deltaMoveY;
            boundary.pos = boundary.pos.add(addVec);
        }

        else {
            Logger.log("Stretch system else called. Shouldn't be here " +info.toString() + " type: "+boundary.boundaryOrient.toString() + " id: "+boundary.identifier);
        }

        return true;
    }

}
