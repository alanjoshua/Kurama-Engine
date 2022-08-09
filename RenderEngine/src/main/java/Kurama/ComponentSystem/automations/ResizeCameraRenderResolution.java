package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.Math.Vector;
import Kurama.camera.Camera;
import Kurama.inputs.Input;

import static Kurama.utils.Logger.log;

public class ResizeCameraRenderResolution implements Automation {

    Camera cam;

    public ResizeCameraRenderResolution(Camera cam) {
        this.cam = cam;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        cam.renderResolution = new Vector(new float[]{current.getWidth(), current.getHeight()});
        cam.setShouldUpdateValues(true);

    }
}
