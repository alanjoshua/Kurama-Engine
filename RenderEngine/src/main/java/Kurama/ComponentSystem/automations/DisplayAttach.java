package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.display.Display;
import Kurama.inputs.Input;

public class DisplayAttach implements Automation {

    public Display display;

    public DisplayAttach(Display display) {
        this.display = display;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        current.setWidth(display.windowResolution.geti(0));
        current.setHeight(display.windowResolution.geti(1));
    }
}
