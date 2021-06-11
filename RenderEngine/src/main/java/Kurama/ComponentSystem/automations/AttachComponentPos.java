package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class AttachComponentPos implements Automation {

    Component comp;
    public AttachComponentPos(Component comp) {
        this.comp = comp;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        current.setPos(comp.getPos());
    }
}
