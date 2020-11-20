package engine.renderingEngine;

public abstract class RenderBlock {

    public String blockID = null;
    public RenderBlock(String id) {
        this.blockID = id;
    }

    public abstract void setup(RenderBlockInput input);
    public abstract void render(RenderBlockInput input);
    public abstract void cleanUp();
}