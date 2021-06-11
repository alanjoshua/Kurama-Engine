package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class PosYTopAttachPercent implements Automation {

    float posYPercent;

    public PosYTopAttachPercent(float posYPercent) {
        this.posYPercent = posYPercent;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        float newY = -current.parent.getHeight() /2f + current.parent.getHeight() *posYPercent + current.getHeight() / 2f;
        current.getPos().setDataElement(1, newY);
    }

    public static float evaluateOnce(Component current, float posYPercent) {
        return -current.parent.getHeight() /2f + current.parent.getHeight() *posYPercent + current.getHeight() / 2f;
    }

}
