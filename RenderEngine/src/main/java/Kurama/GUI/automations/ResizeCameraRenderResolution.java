package Kurama.GUI.automations;

import Kurama.GUI.Component;
import Kurama.Math.Vector;
import Kurama.camera.Camera;

public class ResizeCameraRenderResolution extends Automation {

    Camera cam;

    public ResizeCameraRenderResolution(Camera cam) {
        this.cam = cam;
    }

    @Override
    public void runAutomation(Component current) {
        var curCamRes = cam.renderResolution;
        var newRes = new Vector(new float[]{current.width, current.height});

        // Update camera projection matrices only if resolution has changed
        if(newRes.sub(curCamRes).sumSquared() != 0) {
            cam.renderResolution = newRes;
            cam.setShouldUpdateValues(true);
        }
    }
}
