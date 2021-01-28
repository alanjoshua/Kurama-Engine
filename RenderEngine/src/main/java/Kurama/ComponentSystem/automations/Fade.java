package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class Fade implements Automation {

    float change;

    public Fade(float change) {
        this.change = change;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        current.alphaMask = current.alphaMask - change*timeDelta;
    }
}
