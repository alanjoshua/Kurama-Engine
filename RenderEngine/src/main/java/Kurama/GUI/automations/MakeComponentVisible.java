package Kurama.GUI.automations;

import Kurama.GUI.components.Component;
import Kurama.inputs.Input;

public class MakeComponentVisible implements Automation {

    public Component comp;
    public MakeComponentVisible(Component comp) {
        this.comp = comp;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        comp.isContainerVisible = true;
    }
}
