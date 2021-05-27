package Kurama.ComponentSystem.components.constraintGUI.RigidBodySystem;

import Kurama.ComponentSystem.components.constraintGUI.BoundMoveDataPack;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;

import Kurama.ComponentSystem.components.constraintGUI.Interactor;
import Kurama.Math.Vector;

public class RigidBoundaryInteractor implements Interactor {
    @Override
    public void interact(BoundMoveDataPack data, Boundary boundary, Boundary parentBoundary) {

        if(boundary.boundaryOrient == Boundary.BoundaryOrient.Vertical) {
            if(parentBoundary == null) {
                boundary.pos = boundary.pos.add(new Vector((int) data.deltaMoveX,0, 0));
                data.parentMoveDir = 0;
            }
            else {
                if(data.parentMoveDir == 0) {
                    boundary.pos = boundary.pos.add(new Vector((int) data.deltaMoveX,0, 0));
                }
                else {
                    boundary.pos = boundary.pos.add(new Vector(0, (int) data.deltaMoveY, 0));
                }
            }

            boundary.alreadyUpdated = false; // inverted to save some processing time as this variable would already be set by the recursive verification process

            boundary.negativeAttachments.forEach(b -> {
                if(b.alreadyUpdated) { // inverted to save some processing time as this variable would already be set by the recursive verification process
                    b.interact(data, boundary);
                }
            });
            boundary.positiveAttachments.forEach(b -> {
                if(b.alreadyUpdated)
                    b.interact(data, boundary);
            });
        }
        else { // Horizontal boundary
            if(parentBoundary == null) {
                boundary.pos = boundary.pos.add(new Vector(0, (int) data.deltaMoveY, 0));
                data.parentMoveDir = 1;
            }
            else {
                if(data.parentMoveDir == 0) {
                    boundary.pos = boundary.pos.add(new Vector((int) data.deltaMoveX,0, 0));
                }
                else {
                    boundary.pos = boundary.pos.add(new Vector(0, (int) data.deltaMoveY, 0));
                }
            }

            boundary.alreadyUpdated = false;

            boundary.negativeAttachments.forEach(b -> {
                if(b.alreadyUpdated)
                    b.interact(data, boundary);
            });
            boundary.positiveAttachments.forEach(b -> {
                if(b.alreadyUpdated)
                    b.interact(data, boundary);
            });
        }

    }
}
