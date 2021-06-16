package Kurama.ComponentSystem.components.constraintGUI.stretchSystem;

import Kurama.ComponentSystem.components.constraintGUI.BoundInteractionMessage;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;

public class StretchMessage extends BoundInteractionMessage {

    public boolean shouldValidify = true;
    public boolean shouldOverrideWithinWindowCheck = false;
    public int relativePos = -1;

    public StretchMessage(float deltaMoveX, float deltaMoveY) {
        super(deltaMoveX, deltaMoveY);
    }

    public StretchMessage(float deltaMoveX, float deltaMoveY, Boundary parent, boolean shouldOverrideWithinWindowCheck) {
        super(deltaMoveX, deltaMoveY);
        this.parent = parent;
        this.shouldOverrideWithinWindowCheck = shouldOverrideWithinWindowCheck;
    }

    public StretchMessage(BoundInteractionMessage mess) {
        super(mess);
    }

    public StretchMessage(BoundInteractionMessage mess, Boundary parent) {
        super(parent, mess.deltaMoveX, mess.deltaMoveY);
        if(mess instanceof StretchMessage) {
            this.shouldOverrideWithinWindowCheck = ((StretchMessage) mess).shouldOverrideWithinWindowCheck;
            this.shouldValidify = ((StretchMessage) mess).shouldValidify;
            this.relativePos = ((StretchMessage) mess).relativePos;
        }
    }

    public StretchMessage(StretchMessage mess) {
        super(mess);
        this.shouldOverrideWithinWindowCheck = mess.shouldOverrideWithinWindowCheck;
        this.shouldValidify = mess.shouldValidify;
        this.relativePos = mess.relativePos;
    }

    public StretchMessage(Boundary parent, float deltaMoveX, float deltaMoveY) {
        super(parent, deltaMoveX, deltaMoveY);
    }
}
