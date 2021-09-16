package main;

import Kurama.Math.Vector;
import Kurama.audio.SoundBuffer;
import Kurama.audio.SoundManager;
import Kurama.camera.Camera;
import Kurama.display.Display;
import Kurama.display.DisplayLWJGL;
import Kurama.game.Game;
import Kurama.inputs.InputLWJGL;
import Kurama.renderingEngine.RenderingEngineGL;
import Kurama.renderingEngine.defaultRenderPipeline.DefaultRenderPipeline;
import Kurama.renderingEngine.ginchan.Gintoki;
import Kurama.scene.Scene;
import Kurama.utils.Logger;

import static org.lwjgl.glfw.GLFW.*;

public class MindPalace extends Game {

    public Camera playerCamera;

    public MindPalace(String threadName) {
        super(threadName);
    }

    @Override
    public void init() {
        scene = new Scene(this);

        renderingEngine = new RenderingEngineGL(this);

        display = new DisplayLWJGL(this);
        display.displayMode = Display.DisplayMode.WINDOWED;
        display.startScreen();

        input = new InputLWJGL(this, (DisplayLWJGL) display);

        scene.renderPipeline = new DefaultRenderPipeline(this, null,"sceneRenderer");
        ((RenderingEngineGL)renderingEngine).sceneRenderPipeline = scene.renderPipeline;
        ((RenderingEngineGL)renderingEngine).guiRenderPipeline = new Gintoki(this, null,"Gintoki");
        renderingEngine.init(scene);

        playerCamera = new Camera(this,scene.rootSceneComp, null, new Vector(new float[] {0,7,5}),90, 0.001f, 5000,
                Display.defaultWindowedWidth, Display.defaultWindowedHeight, "playerCam");
        playerCamera.shouldPerformFrustumCulling = true;
        scene.rootSceneComp.addChild(playerCamera);

        scene.cameras.add(playerCamera);

        try {
            var soundManager = new SoundManager();
            soundManager.init();
            scene.soundManager = soundManager;
            SoundBuffer madaraSound = new SoundBuffer("madara", "res/sampleAudio/madaraFirestyle.ogg");
            soundManager.addSoundBuffer(madaraSound);
        }catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        initGUI();
        initScene();

        ((DisplayLWJGL)display).setClearColor(0,0,0,1);
        scene.cameras.forEach(Camera::updateValues);
        targetFPS = display.getRefreshRate();

        Logger.log("");
        for(var mat: scene.materialLibrary) {
            Logger.log(mat.matName);
        }
        Logger.log("");
    }

    public void initGUI() {

    }

    public void initScene() {

    }

    @Override
    public void cleanUp() {
        if(rootGuiComponent != null)rootGuiComponent.cleanUp();
        if(renderingEngine != null) renderingEngine.cleanUp();
        if(scene != null) scene.cleanUp();
    }

    @Override
    public void tick() {
        if(rootGuiComponent != null) {
            rootGuiComponent.tick(null, rootGuiComponent.input, timeDelta, false);
        }

        if(glfwWindowShouldClose(((DisplayLWJGL)display).getWindow())) {
            programRunning = false;
        }

        scene.rootSceneComp.tick(null, input, timeDelta, false);
        scene.soundManager.tick(playerCamera, timeDelta);


        input.reset();

    }

    @Override
    public void render() {
        ((RenderingEngineGL)renderingEngine).render(scene, rootGuiComponent);
        glfwSwapBuffers(((DisplayLWJGL)display).getWindow());
        glfwPollEvents();
        input.poll();
        scene.hasMatLibraryUpdated = false;
    }
}
