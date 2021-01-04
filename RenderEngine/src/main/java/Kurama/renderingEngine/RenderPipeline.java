package Kurama.renderingEngine;

import Kurama.Mesh.Mesh;
import Kurama.game.Game;

import java.util.HashMap;
import java.util.Map;

public abstract class RenderPipeline {

    protected Game game;
    public Map<String, RenderBlock> renderBlockID_renderBlock_map = new HashMap<>();

    public RenderPipeline(Game game) {
        this.game = game;
    }
    public abstract void setup(RenderPipelineInput input);
    public abstract RenderPipelineOutput render(RenderPipelineInput input);
    public abstract void cleanUp();
    public abstract void initializeMesh(Mesh mesh);
//    public abstract void renderResolutionChanged(Vector renderResolution);
}
