package main;

import Kurama.display.DisplayVulkan;
import Kurama.game.Game;
import Kurama.inputs.InputLWJGL;

public class PointCloudController extends Game {

    PointCloudRenderer renderer;

    public PointCloudController(String name) {
        super(name);
    }

    @Override
    public void init() {
        renderingEngine = new PointCloudRenderer(this);
        renderer = (PointCloudRenderer) renderingEngine;

        display = new DisplayVulkan(this);
        display.resizeEvents.add(() -> renderer.windowResized = true);

        renderingEngine.init(null);

        this.setTargetFPS(Integer.MAX_VALUE);
        this.shouldDisplayFPS = true;

        this.input = new InputLWJGL(this, (DisplayVulkan) display);
    }

    @Override
    public void cleanUp() {

    }

    @Override
    public void tick() {

    }

    @Override
    public void render() {

    }
}
