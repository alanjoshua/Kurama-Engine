package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class MaxHeight implements Automation {

    public int maxHeight;

    public MaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        if(current.getHeight() > maxHeight) {
            current.setHeight(maxHeight);
        }
    }
}
