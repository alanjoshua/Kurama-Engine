package engine.renderingEngine;

import engine.Mesh.Mesh;
import engine.game.Game;
import engine.scene.Scene;

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
//    public abstract void initToEndFullRender(Mesh mesh, int offset);
//    public abstract void render(Mesh mesh);
//    public abstract int initRender(Mesh mesh, int offset);
//    public abstract void initRender(Mesh mesh);
//    public abstract void endRender(Mesh mesh);
}
