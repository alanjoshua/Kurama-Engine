package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.automations.Automation;
import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class MinHeight implements Automation {

    public int minHeight;

    public MinHeight(int minHeight) {
        this.minHeight = minHeight;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        if(current.height < minHeight) {
            current.width = minHeight;
        }
    }
}
