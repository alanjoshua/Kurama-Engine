package Kurama.ComponentSystem.components.constraintGUI.interactionConstraints;

import Kurama.ComponentSystem.components.constraintGUI.BoundMoveDataPack;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;

import Kurama.Math.Vector;

public class TemporaryBoundMove implements Interaction {
    @Override
    public void interact(BoundMoveDataPack info, Boundary boundary, Boundary parentBoundary) {

        var data = (TemporaryBoundMoveDataPack)info;

        if(boundary.boundaryOrient == Boundary.BoundaryOrient.Vertical) {
            if(parentBoundary == null) {
                boundary.pos = boundary.pos.add(new Vector((int) data.deltaMove,0, 0));
                data.parentMoveDir = 0;
            }
            else {
                if(data.parentMoveDir == 0) {
                    boundary.pos = boundary.pos.add(new Vector((int) data.deltaMove,0, 0));
                }
                else {
                    boundary.pos = boundary.pos.add(new Vector(0, (int) data.deltaMove, 0));
                }
            }

            boundary.alreadyUpdated = true;

            boundary.negativeAttachments.forEach(b -> {
                if(!b.alreadyUpdated) {
                    b.interact(data, boundary);
                }
            });
            boundary.positiveAttachments.forEach(b -> {
                if(!b.alreadyUpdated)
                    b.interact(data, boundary);
            });
        }
        else {
            if(parentBoundary == null) {
                boundary.pos = boundary.pos.add(new Vector(0, (int) data.deltaMove, 0));
                data.parentMoveDir = 1;
            }
            else {
                if(data.parentMoveDir == 0) {
                    boundary.pos = boundary.pos.add(new Vector((int) data.deltaMove,0, 0));
                }
                else {
                    boundary.pos = boundary.pos.add(new Vector(0, (int) data.deltaMove, 0));
                }
            }

            boundary.alreadyUpdated = true;

            boundary.negativeAttachments.forEach(b -> {
                if(!b.alreadyUpdated)
                    b.interact(data, boundary);
            });
            boundary.positiveAttachments.forEach(b -> {
                if(!b.alreadyUpdated)
                    b.interact(data, boundary);
            });
        }

    }
}
