package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.animations.Animation;
import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class AddAnimation implements Automation {

    public Animation anim;

    public AddAnimation(Animation anim) {
        this.anim = anim;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        anim.hasAnimEnded = false;
        current.addAnimation(anim);
    }
}
