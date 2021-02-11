package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class ToggleKeyboardFocus implements Automation {
    @Override
    public void run(Component current, Input input, float timeDelta) {
        current.isKeyInputFocused = !current.isKeyInputFocused;
    }
}
