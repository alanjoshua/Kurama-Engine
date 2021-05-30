package Kurama.ComponentSystem.components.constraintGUI.stretchSystem;

import Kurama.ComponentSystem.components.constraintGUI.BoundInteractionMessage;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;

public class StretchMessage extends BoundInteractionMessage {

    public boolean shouldValidify = true;

    public StretchMessage(float deltaMoveX, float deltaMoveY) {
        super(deltaMoveX, deltaMoveY);
    }

    public StretchMessage(BoundInteractionMessage mess) {
        super(mess);
    }

    public StretchMessage(Boundary parent, float deltaMoveX, float deltaMoveY) {
        super(parent, deltaMoveX, deltaMoveY);
    }
}
