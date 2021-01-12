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
    public abstract void setup(RenderPipelineInput input);

    public RenderPipelineOutput render(RenderPipelineInput input) {
        RenderPipelineOutput prevOutput = null;
        for(var pipe: renderBlocks) {
            if(prevOutput == null) {
                prevOutput = pipe.render(input);
            }
            else {
                prevOutput = pipe.render(prevOutput.nextInput);
            }
        }
        return prevOutput;
    }
    public abstract void cleanUp();
    public void initializeMesh(Mesh mesh) { }
}
