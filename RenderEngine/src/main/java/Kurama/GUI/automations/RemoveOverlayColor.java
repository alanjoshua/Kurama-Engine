package Kurama.GUI.automations;

import Kurama.GUI.components.Component;
import Kurama.Math.Vector;
import Kurama.inputs.Input;

public class RemoveOverlayColor implements Automation {
    @Override
    public void run(Component current, Input input, float timeDelta) {
        current.overlayColor = new Vector(0,0,0,0);
    }
}
