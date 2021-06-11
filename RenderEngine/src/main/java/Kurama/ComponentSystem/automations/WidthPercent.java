package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class WidthPercent implements Automation {

    public float widthPercent;

    public WidthPercent(float widthPercent) {
        this.widthPercent = widthPercent;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        current.setWidth((int)(current.parent.getWidth() * widthPercent));
    }
}
