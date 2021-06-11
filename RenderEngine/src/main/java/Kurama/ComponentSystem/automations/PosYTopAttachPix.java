package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class PosYTopAttachPix implements Automation {

    public int posYOff;

    public PosYTopAttachPix(int posYOff) {
        this.posYOff = posYOff;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        float newY = -current.parent.getHeight() /2f + posYOff + current.getHeight() / 2f;
        current.getPos().setDataElement(1, newY);
    }
}
