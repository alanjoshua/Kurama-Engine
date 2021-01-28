package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class RemoveAnimations implements Automation {

    @Override
    public void run(Component current, Input input, float timeDelta) {
        for(var anim: current.animations) {
            anim.hasAnimEnded = true;
        }
    }
}
