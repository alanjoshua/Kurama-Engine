package Kurama.GUI.automations;

import Kurama.GUI.Component;
import Kurama.GUI.automations.Automation;
import Kurama.inputs.Input;

public class RemoveOverlayColor implements Automation {
    @Override
    public void run(Component current, Input input) {
        current.overlayColor = null;
    }
}
