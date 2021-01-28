package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class OnlyChildClicked implements Automation {

    @Override
    public void run(Component current, Input input, float timeDelta) {
        if(current.isClicked)  {
            current.parent.isClicked = false;
        }
    }

}
