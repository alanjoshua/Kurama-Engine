package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.animations.Animation;
import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class AddAnimationToComponent implements Automation {

    public Animation anim;
    public Component comp;

    public AddAnimationToComponent(Component comp, Animation anim) {
        this.comp = comp;
        this.anim = anim;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        comp.addAnimation(anim);
    }
}
