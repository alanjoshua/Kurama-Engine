package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.automations.Automation;
import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class HeightPix implements Automation {

    public int height;

    public HeightPix(int height) {
        this.height = height;
    }


    @Override
    public void run(Component current, Input input, float timeDelta) {
        current.height = height;
    }
}
