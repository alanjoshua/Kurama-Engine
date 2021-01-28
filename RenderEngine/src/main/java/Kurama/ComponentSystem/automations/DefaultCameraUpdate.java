package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.camera.Camera;
import Kurama.inputs.Input;

public class DefaultCameraUpdate implements Automation {
    @Override
    public void run(Component current, Input input, float timeDelta) {

        Camera cam = (Camera)current;
        cam.velocity = cam.velocity.add(cam.acceleration.scalarMul(timeDelta));
        var detlaV = cam.velocity.scalarMul(timeDelta);
        cam.pos = cam.pos.add(detlaV);

        if(cam.shouldUpdateValues) {
            cam.updateValues();
            cam.setShouldUpdateValues(false);
        }
    }
}
