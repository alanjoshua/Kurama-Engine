package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class PosXYTopLeftAttachPercent implements Automation {

    float posYPercent;
    float posXPercent;

    public PosXYTopLeftAttachPercent(float posXPercent, float posYPercent) {
        this.posXPercent = posXPercent;
        this.posYPercent = posYPercent;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {

        float newX = -current.parent.getWidth() /2f + current.parent.getWidth() *posXPercent + current.getWidth() / 2f;
        float newY = -current.parent.getHeight() /2f + current.parent.getHeight() *posYPercent + current.getHeight() / 2f;

        current.getPos().setDataElement(0, newX);
        current.getPos().setDataElement(1, newY);
    }
}
