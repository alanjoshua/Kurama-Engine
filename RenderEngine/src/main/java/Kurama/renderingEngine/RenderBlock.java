package Kurama.renderingEngine;

public abstract class RenderBlock {

    public String pipelineID = null;
    public RenderPipeline renderPipeline;
    public RenderBlock(String id, RenderPipeline pipeline) {
        this.pipelineID = id;
        this.renderPipeline = pipeline;
    }

    public abstract void setup(RenderBlockInput input);
    public abstract RenderBlockOutput render(RenderBlockInput input);
    public abstract void cleanUp();
}