package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.Math.Quaternion;
import Kurama.Math.Vector;
import Kurama.inputs.Input;

public class Rotate implements Automation {

    Vector dir;
    float speed;

    public Rotate(Vector dir, float speed) {
        this.dir = dir;
        this.speed = speed;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        Quaternion rot = Quaternion.getAxisAsQuat(dir, speed * timeDelta);
        current.setOrientation(rot.multiply(current.getOrientation()));
    }
}
