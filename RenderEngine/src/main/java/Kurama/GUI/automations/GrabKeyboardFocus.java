package Kurama.GUI.automations;

import Kurama.GUI.components.Component;
import Kurama.inputs.Input;

public class GrabKeyboardFocus implements Automation {
    @Override
    public void run(Component current, Input input, float timeDelta) {
        current.isKeyInputFocused = true;
    }
}
