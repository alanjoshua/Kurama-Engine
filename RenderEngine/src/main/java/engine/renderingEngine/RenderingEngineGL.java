package engine.renderingEngine;

import engine.Math.Matrix;
import engine.scene.Scene;
import engine.game.Game;

import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11C.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11C.glBlendFunc;

public class RenderingEngineGL extends RenderingEngine {

    public RenderPipeline renderPipeline;

    public void init(Scene scene) {

        glEnable(GL_DEPTH_TEST);    //Enables depth testing

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
//        glBlendFunc(GL_SRC_ALPHA, GL_ONE);

        enable(GL_CULL_FACE);
        setCullFace(GL_BACK);

        renderPipeline.setup(scene);

    }

    public RenderingEngineGL(Game game) {
        super(game);
    }

    public void enableModelOutline() {
        glPolygonMode(GL_FRONT_AND_BACK,GL_LINE);
    }

    public void enableModelFill() {
        glPolygonMode(GL_FRONT_AND_BACK,GL_TRIANGLES);
    }

    public static void clear() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    public void render(Scene scene) {
        renderPipeline.render(scene);
    }

    public void enable(int param) {
        glEnable(param);
    }

    public void disable(int param) {
        glDisable(param);
    }

    public void setCullFace(int param) {
        glCullFace(param);
    }

    public class ShadowDepthRenderPackage {
        public List<Matrix> worldToDirectionalLights;
        public List<Matrix> worldToSpotLights;
        public ShadowDepthRenderPackage(List<Matrix> worldToDirectionalLights,List<Matrix> worldToSpotLights) {
            this.worldToDirectionalLights = worldToDirectionalLights;
            this.worldToSpotLights = worldToSpotLights;
        }
    }

    public void cleanUp() {
        renderPipeline.cleanUp();
    }

}

