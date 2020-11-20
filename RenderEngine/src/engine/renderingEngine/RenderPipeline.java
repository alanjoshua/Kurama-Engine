package engine.renderingEngine;

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
}
