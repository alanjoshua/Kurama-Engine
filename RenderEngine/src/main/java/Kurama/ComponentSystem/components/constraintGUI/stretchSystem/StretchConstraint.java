package Kurama.ComponentSystem.components.constraintGUI.stretchSystem;

import Kurama.ComponentSystem.components.constraintGUI.BoundMoveDataPack;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;
import Kurama.ComponentSystem.components.constraintGUI.interactionConstraints.InteractionConstraint;

public class StretchConstraint implements InteractionConstraint {
    @Override
    public boolean isValid(Boundary boundary, BoundMoveDataPack info) {

        if(boundary.boundaryOrient == Boundary.BoundaryOrient.Vertical) {

        }
        // Horizontal
        else {

            // moving down
            if(info.deltaMoveY >= 0) {

            }
        }

        return true;
    }
}
