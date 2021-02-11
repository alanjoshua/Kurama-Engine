package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.automations.Automation;
import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class MinPosX implements Automation {

    int minPos;

    public MinPosX(int minPos) {
        this.minPos = minPos;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        if(current.pos.get(0) - current.width/2f < minPos) {
            current.pos.setDataElement(0, minPos + current.width/2f);
        }
    }
}