package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class MaxPosX implements Automation {

    int maxPos;

    public MaxPosX(int maxPos) {
        this.maxPos = maxPos;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        if(current.getPos().get(0) + current.getWidth() /2f > maxPos) {
            current.getPos().setDataElement(0, maxPos - current.getWidth() /2f);
        }
    }
}
