package Kurama.renderingEngine;

import Kurama.buffers.RenderBuffer;
import Kurama.game.Game;
import Kurama.scene.Scene;

public class RenderBufferRenderBlockInput extends RenderBlockInput {

    public RenderBuffer renderBuffer;

    public RenderBufferRenderBlockInput(Scene scene, Game game, RenderBlockOutput previousOutput, RenderBuffer renderBuffer) {
        super(scene, game, previousOutput);
        this.renderBuffer = renderBuffer;
    }
}
