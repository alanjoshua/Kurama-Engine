package Kurama.GUI.automations;

import Kurama.GUI.components.Component;
import Kurama.inputs.Input;

public class RemoveAnimationsFromComponent implements Automation {

    public Component comp;
    public RemoveAnimationsFromComponent(Component comp) {
        this.comp = comp;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        comp.animations.clear();
    }
}