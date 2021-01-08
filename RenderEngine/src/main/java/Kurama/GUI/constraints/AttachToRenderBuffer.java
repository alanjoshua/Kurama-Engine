package Kurama.GUI.constraints;

import Kurama.GUI.Component;
import Kurama.Mesh.Texture;
import Kurama.buffers.RenderBuffer;

public class AttachToRenderBuffer implements Constraint {

    public RenderBuffer renderBuffer;

    public AttachToRenderBuffer(RenderBuffer renderBuffer) {
        this.renderBuffer = renderBuffer;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        current.width = renderBuffer.renderResolution.geti(0);
        current.height = renderBuffer.renderResolution.geti(1);

        if(current.texture == null) {
            current.texture = new Texture(renderBuffer.textureId);
        }
        else {
            current.texture.id = renderBuffer.textureId;
        }

    }
}
