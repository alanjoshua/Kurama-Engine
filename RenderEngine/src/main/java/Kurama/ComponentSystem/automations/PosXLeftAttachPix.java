package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class PosXLeftAttachPix implements Automation {

    public int posXOff;

    public PosXLeftAttachPix(int posXOff) {
        this.posXOff = posXOff;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        float newX = -current.parent.getWidth() /2f + posXOff + current.getWidth() / 2f;
        current.getPos().setDataElement(0, newX);
    }
}
