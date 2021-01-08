package Kurama.GUI.inputHandling;

import Kurama.GUI.Component;
import Kurama.inputs.Input;

public interface InputAction {

    void run(Component current, Input input);

}
