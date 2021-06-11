package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class WidthHeightPercent implements Automation {

    public float widthPercent;
    public float heightPercent;

    public WidthHeightPercent(float widthPercent, float heightPercent) {
        this.widthPercent = widthPercent;
        this.heightPercent = heightPercent;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        current.setWidth((int)(current.parent.getWidth() * widthPercent));
        current.setHeight((int)(current.parent.getHeight() * heightPercent));
    }
}
