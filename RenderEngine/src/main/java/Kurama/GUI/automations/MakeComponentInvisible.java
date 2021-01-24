package Kurama.GUI.automations;

import Kurama.GUI.components.Component;
import Kurama.inputs.Input;

public class MakeComponentInvisible implements Automation {

    public Component comp;
    public MakeComponentInvisible(Component comp) {
        this.comp = comp;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        comp.isContainerVisible = false;
    }
}
