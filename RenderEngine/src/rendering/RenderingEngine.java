package rendering;

import main.Game;

public abstract class RenderingEngine {

    protected Game game;

    public enum ProjectionMode {
        ORTHO, PERSPECTIVE
    }

    public enum RenderPipeline {
        Matrix, Quat
    }

    protected ProjectionMode projectionMode = ProjectionMode.PERSPECTIVE;
    protected RenderPipeline renderPipeline = RenderPipeline.Matrix;

    public RenderingEngine(Game game) {
        this.game = game;
    }

    public abstract void init();
    public abstract void cleanUp();

    public ProjectionMode getProjectionMode() {
        return projectionMode;
    }

    public void setProjectionMode(ProjectionMode projectionMode) {
        this.projectionMode = projectionMode;
    }

    public RenderPipeline getRenderPipeline() {
        return renderPipeline;
    }

    public void setRenderPipeline(RenderPipeline renderPipeline) {
        this.renderPipeline = renderPipeline;
    }
}
