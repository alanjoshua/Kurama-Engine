package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.automations.Automation;
import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class PosXLeftAttachPercent implements Automation {

    float posXPercent;

    public PosXLeftAttachPercent(float posXPercent) {
        this.posXPercent = posXPercent;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        float newX = -current.parent.width/2f + current.parent.width*posXPercent + current.width / 2f;
        current.pos.setDataElement(0, newX);
    }
}
