package Kurama.ComponentSystem.components.constraintGUI;

import Kurama.Math.Vector;

public class DefaultBoundaryInteractor implements Interactor {

    @Override
    public boolean interact(BoundInteractionMessage info, Boundary boundary, Boundary parentBoundary, int relativePos) {

        if(boundary.boundaryOrient == Boundary.BoundaryOrient.Vertical) {
            boundary.setPos(boundary.getPos().add(new Vector(info.deltaMoveX, 0f, 0f)));
        }
        else {
            boundary.setPos(boundary.getPos().add(new Vector(0f, info.deltaMoveY, 0f)));
        }

        return true;
    }
}
