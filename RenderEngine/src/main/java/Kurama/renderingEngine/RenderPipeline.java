package Kurama.renderingEngine;

import Kurama.Mesh.Mesh;
import Kurama.game.Game;

import java.util.ArrayList;
import java.util.List;

public abstract class RenderPipeline {

    protected Game game;
    public String pipelineID = null;
    public RenderPipeline parentPipeline;
    public List<RenderPipeline> renderBlocks = new ArrayList<>();

    public RenderPipeline(Game game, RenderPipeline parentPipeline, String pipelineID) {
        this.game = game;
        this.parentPipeline = parentPipeline;
        this.pipelineID = pipelineID;
    }
    public abstract void setup(RenderPipelineData input);

    public RenderPipelineData render(RenderPipelineData input) {
        for(var pipe: renderBlocks) {
            input = pipe.render(input);
        }
        return input;
    }
    public abstract void cleanUp();
    public void initializeMesh(Mesh mesh) { }
}
