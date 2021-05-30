package Kurama.ComponentSystem.components.constraintGUI.stretchSystem;

import Kurama.ComponentSystem.components.constraintGUI.BoundInteractionMessage;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;
import Kurama.ComponentSystem.components.constraintGUI.IVRequestPackGenerator;

public class StretchIVRG implements IVRequestPackGenerator {
    @Override
    public BoundInteractionMessage getValidificationRequestPack(Boundary parent, Boundary boundary, float deltaMoveX, float deltaMoveY) {

        var mess = new StretchMessage(parent, deltaMoveX, deltaMoveY);
        mess.shouldValidify = true;

        if(boundary.boundaryOrient == Boundary.BoundaryOrient.Horizontal) {
            mess.deltaMoveX = 0;
        }
        else {
            mess.deltaMoveY = 0;
        }

        return mess;
    }
}
