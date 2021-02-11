package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class MaxPosY implements Automation {

    int maxPos;

    public MaxPosY(int maxPos) {
        this.maxPos = maxPos;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        if(current.pos.get(1) + current.height/2f> maxPos) {
            current.pos.setDataElement(1, maxPos - current.height/2f);
        }
    }
}