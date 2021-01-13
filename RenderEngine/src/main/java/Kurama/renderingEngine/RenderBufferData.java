package Kurama.renderingEngine;

import Kurama.buffers.RenderBuffer;
import Kurama.game.Game;
import Kurama.scene.Scene;

public class RenderBufferData extends RenderPipelineData {

    public RenderBuffer renderBuffer;

    public RenderBufferData(Scene scene, Game game, RenderBuffer renderBuffer) {
        super(scene, game);
        this.renderBuffer = renderBuffer;
    }
}
