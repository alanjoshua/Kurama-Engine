package Kurama.GUI.automations;

import Kurama.GUI.Component;
import Kurama.inputs.Input;

public interface Automation {

    public abstract void run(Component current, Input input);

}
