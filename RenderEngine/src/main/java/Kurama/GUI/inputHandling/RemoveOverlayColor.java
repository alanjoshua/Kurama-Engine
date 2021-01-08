package Kurama.GUI.inputHandling;

import Kurama.GUI.Component;
import Kurama.inputs.Input;

public class RemoveOverlayColor implements InputAction {
    @Override
    public void run(Component current, Input input) {
        current.overlayColor = null;
    }
}
