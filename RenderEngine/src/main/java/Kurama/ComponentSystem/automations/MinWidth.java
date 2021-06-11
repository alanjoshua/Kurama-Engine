package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class MinWidth implements Automation {

    public int minWidth;

    public MinWidth(int minWidth) {
        this.minWidth = minWidth;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        if(current.getWidth() < minWidth) {
            current.setWidth(minWidth);
        }
    }
}
