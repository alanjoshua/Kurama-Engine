package Kurama.ComponentSystem.components.constraintGUI.interactionConstraints;

import Kurama.ComponentSystem.components.constraintGUI.BoundInteractionMessage;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;

public class MaxXPos implements InteractionConstraint {

    public float maxXPos;

    // relative to parent
    public MaxXPos(float maxPos) {
        maxXPos = maxPos;
    }

    @Override
    public boolean isValid(Boundary boundary, BoundInteractionMessage info) {

        if(info.parentMoveDir == 1) {
            return true; // Don't check anything if not moving vertically
        }

        float cur = boundary.pos.get(0) + boundary.width/2f + boundary.parent.width/2f + info.deltaMoveX;
        float max = boundary.parent.width * maxXPos;

        if(cur >= max) {
            return false;
        }
        else {
            return true;
        }

    }
}
