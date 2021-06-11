package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class Fit implements Automation {

    @Override
    public void run(Component current, Input input, float timeDelta) {
        float aspectRatio = (float) current.getWidth() / (float) current.getHeight();

        if(aspectRatio < 1) { // width is less than height
            current.setHeight(current.parent.getHeight());
            current.setWidth((int) ((float) current.getHeight() * aspectRatio));
        }
        else {
            current.setWidth(current.parent.getWidth());
            current.setHeight((int)((float) current.getWidth() / aspectRatio));
        }

    }
}
