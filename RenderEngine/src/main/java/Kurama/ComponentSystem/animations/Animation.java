package Kurama.ComponentSystem.animations;

import Kurama.ComponentSystem.automations.Automation;
import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

import java.util.Arrays;
import java.util.List;

public class Animation {

    public Float animTime;
    public List<Automation> actions;
    public List<Automation> onAnimStart;
    public List<Automation> onAnimEnd;
    public float timeSinceStart = 0;
    public boolean hasAnimEnded = false;

    public Animation(float animTime, List<Automation> actions, List<Automation> onAnimStart, List<Automation> onAnimEnd) {
        this.animTime = animTime;
        this.actions = actions;
        this.onAnimStart = onAnimStart;
        this.onAnimEnd = onAnimEnd;
    }

    public Animation(float animTime, Automation action) {
        this.animTime = animTime;
        this.actions = Arrays.asList(new Automation[]{action});
        this.onAnimStart = null;
        this.onAnimEnd = null;
    }

    public void run(Component current, Input input, float timeDelta) {

        // animation start
        if(timeSinceStart == 0) {
            if(onAnimStart != null) {
                for (var action : onAnimStart) {
                    action.run(current, input, timeDelta);
                }
            }
        }

        // running animation
        else if(timeSinceStart < animTime) {
            if(actions != null) {
                for (var action : actions) {
                    action.run(current, input, timeDelta);
                }
            }
        }

        // anim end
        else {
            if(onAnimEnd != null) {
                for (var action : onAnimEnd) {
                    action.run(current, input, timeDelta);
                }
            }
            hasAnimEnded = true;  // The goal is for the component to discard the animation once it has ended
        }

        timeSinceStart += timeDelta;

    }

}
