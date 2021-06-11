package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class PosXLeftAttachPercent implements Automation {

    float posXPercent;

    public PosXLeftAttachPercent(float posXPercent) {
        this.posXPercent = posXPercent;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        float newX = -current.parent.getWidth() /2f + current.parent.getWidth() *posXPercent + current.getWidth() / 2f;
        current.getPos().setDataElement(0, newX);
    }

    public static float evaluateOnce(Component current, float posXPercent) {
        return -current.parent.getWidth() /2f + current.parent.getWidth() *posXPercent + current.getWidth() / 2f;
    }

}
