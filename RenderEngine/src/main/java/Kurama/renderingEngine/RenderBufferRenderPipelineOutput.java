package Kurama.renderingEngine;

import Kurama.buffers.RenderBuffer;

public class RenderBufferRenderPipelineOutput extends RenderPipelineOutput{

    public RenderBuffer buffer;

    public RenderBufferRenderPipelineOutput(RenderBuffer buffer) {
        this.buffer = buffer;
    }

}
