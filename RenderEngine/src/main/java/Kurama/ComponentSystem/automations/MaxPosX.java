package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.automations.Automation;
import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class MaxPosX implements Automation {

    int maxPos;

    public MaxPosX(int maxPos) {
        this.maxPos = maxPos;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        if(current.pos.get(0) + current.width/2f > maxPos) {
            current.pos.setDataElement(0, maxPos - current.width/2f);
        }
    }
}
