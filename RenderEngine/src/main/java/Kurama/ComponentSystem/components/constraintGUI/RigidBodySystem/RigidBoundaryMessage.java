package Kurama.ComponentSystem.components.constraintGUI.RigidBodySystem;

import Kurama.ComponentSystem.components.constraintGUI.BoundInteractionMessage;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;

public class RigidBoundaryMessage extends BoundInteractionMessage {

    public boolean shouldValidify = true;

    public RigidBoundaryMessage(BoundInteractionMessage mess) {
        super(mess);
    }
    public RigidBoundaryMessage(RigidBoundaryMessage mess) {
        super(mess);
        shouldValidify = mess.shouldValidify;
    }

    public RigidBoundaryMessage(Boundary parent, float deltaMoveX, float deltaMoveY, int parentMoveDir) {
        super(parent, deltaMoveX, deltaMoveY, parentMoveDir);
    }
}
