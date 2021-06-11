package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class HeightPercent implements Automation {

    public float heightPercent;

    public HeightPercent(float heightPercent) {
        this.heightPercent = heightPercent;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        current.setHeight((int)(current.parent.getHeight() * heightPercent));
    }
}
