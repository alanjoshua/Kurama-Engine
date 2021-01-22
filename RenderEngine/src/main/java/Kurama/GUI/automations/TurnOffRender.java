package Kurama.GUI.automations;

import Kurama.GUI.components.Component;
import Kurama.inputs.Input;

public class TurnOffRender implements Automation {
    @Override
    public void run(Component current, Input input, float timeDelta) {
        current.shouldRenderGroup = false;
    }
}
