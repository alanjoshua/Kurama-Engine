package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.automations.Automation;
import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class MaxWidth implements Automation {

    public int maxWidth;

    public MaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        if(current.width > maxWidth) {
            current.width = maxWidth;
        }
    }
}
