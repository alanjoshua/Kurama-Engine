package Kurama.GUI.automations;

import Kurama.GUI.components.Component;
import Kurama.inputs.Input;

public class OnlyChildClicked implements Automation {

    @Override
    public void run(Component current, Input input, float timeDelta) {
        if(current.isClicked)  {
            current.parent.isClicked = false;
        }
    }

}
