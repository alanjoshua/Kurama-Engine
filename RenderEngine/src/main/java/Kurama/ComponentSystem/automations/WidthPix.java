package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.automations.Automation;
import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class WidthPix implements Automation {

    public int width;

    public WidthPix(int width) {
        this.width = width;
    }


    @Override
    public void run(Component current, Input input, float timeDelta) {
        current.width = width;
    }
}
