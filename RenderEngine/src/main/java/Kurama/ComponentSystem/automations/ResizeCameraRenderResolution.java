package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.Math.Vector;
import Kurama.camera.Camera;
import Kurama.inputs.Input;

public class ResizeCameraRenderResolution implements Automation {

    Camera cam;

    public ResizeCameraRenderResolution(Camera cam) {
        this.cam = cam;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        var curCamRes = cam.renderResolution;
        var newRes = new Vector(new float[]{current.width, current.height});

        // Update camera projection matrices only if resolution has changed
        if(newRes.sub(curCamRes).sumSquared() != 0) {
            cam.renderResolution = newRes;
            cam.setShouldUpdateValues(true);
        }
    }
}
