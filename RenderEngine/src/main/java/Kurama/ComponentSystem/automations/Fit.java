package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.automations.Automation;
import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class Fit implements Automation {

    @Override
    public void run(Component current, Input input, float timeDelta) {
        float aspectRatio = (float) current.width / (float) current.height;

        if(aspectRatio < 1) { // width is less than height
            current.height = current.parent.height;
            current.width = (int) ((float)current.height * aspectRatio);
        }
        else {
            current.width = current.parent.width;
            current.height = (int)((float)current.width / aspectRatio);
        }

    }
}
