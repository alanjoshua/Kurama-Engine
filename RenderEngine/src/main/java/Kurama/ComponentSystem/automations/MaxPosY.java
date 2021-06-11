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
        if(current.getPos().get(1) + current.getHeight() /2f> maxPos) {
            current.getPos().setDataElement(1, maxPos - current.getHeight() /2f);
        }
    }
}