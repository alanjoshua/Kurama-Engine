package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.inputs.Input;

public class Blink implements Automation {

    public float blinkSpeed;
    public float timeSinceLastBlink = 0;

    public Blink(float blinkSpeed) {
        this.blinkSpeed = blinkSpeed;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {

        if(timeSinceLastBlink > blinkSpeed) {
            current.isContainerVisible = !current.isContainerVisible;  //blink
            timeSinceLastBlink = 0;
        }
        timeSinceLastBlink += timeDelta;
    }
}
