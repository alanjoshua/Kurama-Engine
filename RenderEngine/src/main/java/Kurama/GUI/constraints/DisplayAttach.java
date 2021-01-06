package Kurama.GUI.constraints;

import Kurama.GUI.Component;
import Kurama.display.Display;

public class DisplayAttach extends Constraint {

    public Display display;

    public DisplayAttach(Display display) {
        this.display = display;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        current.width = display.windowResolution.geti(0);
        current.height = display.windowResolution.geti(1);
    }
}
