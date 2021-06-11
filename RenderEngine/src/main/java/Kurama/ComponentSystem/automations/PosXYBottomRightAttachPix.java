package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class PosXYBottomRightAttachPix implements Automation {

    public int posXOff;
    public int posYOff;

    public PosXYBottomRightAttachPix(int posXOff, int posYOff) {
        this.posXOff = posXOff;
        this.posYOff = posYOff;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        float newX = current.parent.getWidth() /2f - (posXOff + current.getWidth() / 2f);
        float newY = current.parent.getHeight() /2f - (posYOff + current.getHeight() / 2f);
        current.getPos().setDataElement(0, newX);
        current.getPos().setDataElement(1, newY);
    }

}
