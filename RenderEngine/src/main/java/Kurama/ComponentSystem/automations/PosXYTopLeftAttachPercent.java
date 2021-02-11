package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.automations.Automation;
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

        float newX = -current.parent.width/2f + current.parent.width*posXPercent + current.width / 2f;
        float newY = -current.parent.height/2f + current.parent.height*posYPercent + current.height / 2f;

        current.pos.setDataElement(0, newX);
        current.pos.setDataElement(1, newY);
    }
}
