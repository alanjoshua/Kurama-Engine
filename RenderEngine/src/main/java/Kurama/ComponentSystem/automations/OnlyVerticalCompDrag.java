package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class OnlyVerticalCompDrag implements Automation {
    @Override
    public void run(Component current, Input input, float timeDelta) {
        current.pos.setDataElement(0, current.pos.get(0) + input.mouseDx);
    }
}
