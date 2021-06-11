package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class MinPosY implements Automation {

    int minPos;

    public MinPosY(int minPos) {
        this.minPos = minPos;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        if(current.getPos().get(1) - current.getHeight() /2f < minPos) {
            current.getPos().setDataElement(1, minPos + current.getHeight() /2f);
        }
    }
}
