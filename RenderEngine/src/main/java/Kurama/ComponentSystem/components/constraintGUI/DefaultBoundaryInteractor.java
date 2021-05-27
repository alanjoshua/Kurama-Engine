package Kurama.ComponentSystem.components.constraintGUI;

import Kurama.Math.Vector;

public class DefaultBoundaryInteractor implements Interactor {

    @Override
    public void interact(BoundMoveDataPack info, Boundary boundary, Boundary parentBoundary) {

        if(boundary.boundaryOrient == Boundary.BoundaryOrient.Vertical) {
            boundary.pos = boundary.pos.add(new Vector(info.deltaMoveX, 0f, 0f));
        }
        else {
            boundary.pos = boundary.pos.add(new Vector(0f, info.deltaMoveY, 0f));
        }

    }
}
