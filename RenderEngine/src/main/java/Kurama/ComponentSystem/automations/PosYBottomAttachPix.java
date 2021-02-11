package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.automations.Automation;
import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class PosYBottomAttachPix implements Automation {

    public int posYOff;

    public PosYBottomAttachPix(int posYOff) {
        this.posYOff = posYOff;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        float newY = current.parent.height/2f - (posYOff + current.height / 2f);
        current.pos.setDataElement(1, newY);
    }

}