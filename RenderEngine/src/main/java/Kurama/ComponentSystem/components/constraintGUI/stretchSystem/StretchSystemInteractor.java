package Kurama.ComponentSystem.components.constraintGUI.stretchSystem;

import Kurama.ComponentSystem.components.constraintGUI.BoundMoveDataPack;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;
import Kurama.ComponentSystem.components.constraintGUI.Interactor;
import Kurama.Math.Vector;

public class StretchSystemInteractor implements Interactor {
    @Override
    public void interact(BoundMoveDataPack info, Boundary boundary, Boundary parentBoundary) {

        if(boundary.boundaryOrient == Boundary.BoundaryOrient.Vertical) {
            boundary.pos = boundary.pos.add(new Vector(info.deltaMoveX, 0, 0));

            var addVec = new Vector(info.deltaMoveX/2f, 0, 0);
            for(var b: boundary.positiveAttachments) {
                b.width -= info.deltaMoveX;
                b.pos = b.pos.add(addVec);
            }
            for(var b: boundary.negativeAttachments) {
                b.width += info.deltaMoveX;
                b.pos = b.pos.add(addVec);
            }

        }
        else {
            boundary.pos = boundary.pos.add(new Vector(0, info.deltaMoveY, 0));

            var addVec = new Vector(0, info.deltaMoveY/2f, 0);
            for(var b: boundary.positiveAttachments) {
                b.height += info.deltaMoveY;
                b.pos = b.pos.add(addVec);
            }
            for(var b: boundary.negativeAttachments) {
                b.height -= info.deltaMoveY;
                b.pos = b.pos.add(addVec);
            }
        }

    }
}
