package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.automations.Automation;
import Kurama.ComponentSystem.components.Component;
import Kurama.Math.Vector;
import Kurama.inputs.Input;

public class Center implements Automation {
    @Override
    public void run(Component current, Input input, float timeDelta) {
        current.pos = new Vector(3, 0);
    }
}
