package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public interface Automation {

    public abstract void run(Component current, Input input, float timeDelta);

}
