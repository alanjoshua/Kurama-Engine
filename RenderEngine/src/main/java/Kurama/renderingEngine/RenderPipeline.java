package Kurama.renderingEngine;

import Kurama.Math.Vector;
import Kurama.Mesh.Mesh;
import Kurama.game.Game;
import Kurama.scene.Scene;

import java.util.HashMap;
import java.util.Map;

public abstract class RenderPipeline {

    protected Game game;
    public Map<String, RenderBlock> renderBlockID_renderBlock_map = new HashMap<>();

    public RenderPipeline(Game game) {
        this.game = game;
    }
    public abstract void setup(Scene scene);
    public abstract void render(Scene scene);
    public abstract void cleanUp();
    public abstract void initializeMesh(Mesh mesh);
    public abstract void renderResolutionChanged(Vector renderResolution);
}
