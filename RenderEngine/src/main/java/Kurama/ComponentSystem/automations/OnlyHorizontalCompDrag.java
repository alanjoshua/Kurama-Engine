package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class OnlyHorizontalCompDrag implements Automation {
    @Override
    public void run(Component current, Input input, float timeDelta) {
        current.getPos().setDataElement(1, current.getPos().get(1) + input.mouseDy);
    }
}
