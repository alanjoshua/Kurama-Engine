package Kurama.GUI.automations;

import Kurama.Math.Vector;
import Kurama.inputs.Input;

public class SetOverlayColor implements Automation {

    public Vector overlayColor;
    public SetOverlayColor(Vector overlayColor) {
        this.overlayColor = overlayColor;
    }

    @Override
    public void run(Kurama.GUI.Component current, Input input) {
        current.overlayColor = overlayColor;
    }
}
