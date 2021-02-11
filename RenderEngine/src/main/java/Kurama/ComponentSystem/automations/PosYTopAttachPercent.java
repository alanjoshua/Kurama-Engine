package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.automations.Automation;
import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class PosYTopAttachPercent implements Automation {

    float posYPercent;

    public PosYTopAttachPercent(float posYPercent) {
        this.posYPercent = posYPercent;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        float newY = -current.parent.height/2f + current.parent.height*posYPercent + current.height / 2f;
        current.pos.setDataElement(1, newY);
    }
}
