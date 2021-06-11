package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.Math.Vector;
import Kurama.buffers.RenderBuffer;
import Kurama.inputs.Input;

public class ResizeRenderBuffer implements Automation {

    RenderBuffer buffer;

    public ResizeRenderBuffer(RenderBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        var curCamRes = buffer.renderResolution;
        var newRes = new Vector(new float[]{current.getWidth(), current.getHeight()});

        // Update camera projection matrices only if resolution has changed
        if(newRes.sub(curCamRes).sumSquared() != 0) {
            buffer.resizeTexture(newRes);
        }
    }
}
