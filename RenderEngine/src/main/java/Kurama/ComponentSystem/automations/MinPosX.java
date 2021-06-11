package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class MinPosX implements Automation {

    int minPos;

    public MinPosX(int minPos) {
        this.minPos = minPos;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        if(current.getPos().get(0) - current.getWidth() /2f < minPos) {
            current.getPos().setDataElement(0, minPos + current.getWidth() /2f);
        }
    }
}