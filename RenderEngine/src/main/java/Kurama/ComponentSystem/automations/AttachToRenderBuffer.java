package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.Mesh.Texture;
import Kurama.buffers.RenderBuffer;
import Kurama.inputs.Input;

public class AttachToRenderBuffer implements Automation {

    public RenderBuffer renderBuffer;

    public AttachToRenderBuffer(RenderBuffer renderBuffer) {
        this.renderBuffer = renderBuffer;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        current.setWidth(renderBuffer.renderResolution.geti(0));
        current.setHeight(renderBuffer.renderResolution.geti(1));

        if(current.texture == null) {
            current.texture = new Texture(renderBuffer.textureId);
        }
        else {
            current.texture.id = renderBuffer.textureId;
        }

    }
}
