package Kurama.GUI.automations;

import Kurama.GUI.components.Component;
import Kurama.Math.Vector;
import Kurama.inputs.Input;

public class SetOverlayColor implements Automation {

    public Vector overlayColor;
    public SetOverlayColor(Vector overlayColor) {
        this.overlayColor = overlayColor;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        current.overlayColor = overlayColor;
    }
}
