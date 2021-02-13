package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class PosYBottomAttachPercent implements Automation {

    float posYPercent;

    public PosYBottomAttachPercent(float posYPercent) {
        this.posYPercent = posYPercent;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        float newY = current.parent.height/2f - (current.parent.height*posYPercent + current.height / 2f);
        current.pos.setDataElement(1, newY);
    }

    public static float evaluateOnce(Component current, float posYPercent) {
        return current.parent.height/2f - (current.parent.height*posYPercent + current.height / 2f);
    }

}
