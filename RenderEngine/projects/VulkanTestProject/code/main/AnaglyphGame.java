package main;

import Kurama.game.Game;

// Reference from https://github.com/SaschaWillems/Vulkan/blob/master/examples/multiview/multiview.cpp
// and https://quiescentspark.blogspot.com/search/label/stereoscopic

public class AnaglyphGame extends Game {

    public AnaglyphRenderer renderer;

    public AnaglyphGame(String threadName) {
        super(threadName);
    }

    @Override
    public void init() {
        renderingEngine = new AnaglyphRenderer(this);
        renderer = (AnaglyphRenderer)renderingEngine;

        initVulkan();
    }

    public void initVulkan() {

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
