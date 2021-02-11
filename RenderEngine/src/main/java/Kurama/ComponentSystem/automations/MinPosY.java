package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.automations.Automation;
import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class MinPosY implements Automation {

    int minPos;

    public MinPosY(int minPos) {
        this.minPos = minPos;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        if(current.pos.get(1) - current.height/2f < minPos) {
            current.pos.setDataElement(1, minPos + current.height/2f);
        }
    }
}
