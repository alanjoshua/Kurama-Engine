package Kurama.renderingEngine;

public abstract class RenderBlock {

    public String blockID = null;
    public RenderPipeline renderPipeline;
    public RenderBlock(String id, RenderPipeline pipeline) {
        this.blockID = id;
        this.renderPipeline = pipeline;
    }

    public abstract void setup(RenderBlockInput input);
    public abstract void render(RenderBlockInput input);
    public abstract void cleanUp();
}