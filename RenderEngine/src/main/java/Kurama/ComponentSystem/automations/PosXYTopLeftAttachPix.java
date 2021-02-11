package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.automations.Automation;
import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class PosXYTopLeftAttachPix implements Automation {

    public int posXOff;
    public int posYOff;

    public PosXYTopLeftAttachPix(int posXOff, int posYOff) {
        this.posXOff = posXOff;
        this.posYOff = posYOff;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {

        float newX = -current.parent.width/2f + posXOff + current.width / 2f;
        float newY = -current.parent.height/2f + posYOff + current.height / 2f;

        current.pos.setDataElement(0, newX);
        current.pos.setDataElement(1, newY);
    }
}
