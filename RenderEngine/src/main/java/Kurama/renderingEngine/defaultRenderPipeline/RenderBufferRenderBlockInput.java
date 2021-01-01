package Kurama.renderingEngine.defaultRenderPipeline;

import Kurama.buffers.RenderBuffer;
import Kurama.game.Game;
import Kurama.renderingEngine.RenderBlockInput;
import Kurama.scene.Scene;

public class RenderBufferRenderBlockInput extends RenderBlockInput {

    public RenderBuffer renderBuffer;

    public RenderBufferRenderBlockInput(Scene scene, Game game, RenderBuffer renderBuffer) {
        super(scene, game);
        this.renderBuffer = renderBuffer;
    }
}
