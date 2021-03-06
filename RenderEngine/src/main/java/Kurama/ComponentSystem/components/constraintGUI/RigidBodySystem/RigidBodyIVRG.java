package Kurama.ComponentSystem.components.constraintGUI.RigidBodySystem;

import Kurama.ComponentSystem.components.constraintGUI.BoundInteractionMessage;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;
import Kurama.ComponentSystem.components.constraintGUI.IVRequestPackGenerator;

public class RigidBodyIVRG implements IVRequestPackGenerator {
    @Override
    public BoundInteractionMessage getValidificationRequestPack(Boundary parent, Boundary boundary, float deltaMoveX, float deltaMoveY) {

        if(boundary.boundaryOrient == Boundary.BoundaryOrient.Vertical) {
            return new RigidBoundaryMessage(parent, deltaMoveX, 0, -1);
        }
        else {
            return new RigidBoundaryMessage(parent,0, deltaMoveY, -1);
        }
    }
}
