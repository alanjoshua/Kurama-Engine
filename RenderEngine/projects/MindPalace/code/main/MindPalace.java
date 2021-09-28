package main;

import Kurama.ComponentSystem.automations.GrabKeyboardFocus;
import Kurama.ComponentSystem.automations.ResizeCameraRenderResolution;
import Kurama.ComponentSystem.automations.WidthHeightPercent;
import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.components.MasterWindow;
import Kurama.ComponentSystem.components.Rectangle;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;
import Kurama.ComponentSystem.components.constraintGUI.BoundaryConfigurator;
import Kurama.ComponentSystem.components.constraintGUI.stretchSystem.StretchSystemConfigurator;
import Kurama.ComponentSystem.components.model.Model;
import Kurama.Math.Vector;
import Kurama.Mesh.Material;
import Kurama.Mesh.Texture;
import Kurama.audio.SoundBuffer;
import Kurama.audio.SoundManager;
import Kurama.camera.Camera;
import Kurama.display.Display;
import Kurama.display.DisplayLWJGL;
import Kurama.game.Game;
import Kurama.geometry.MeshBuilder;
import Kurama.geometry.MeshBuilderHints;
import Kurama.inputs.Input;
import Kurama.inputs.InputLWJGL;
import Kurama.renderingEngine.RenderingEngine;
import Kurama.renderingEngine.RenderingEngineGL;
import Kurama.renderingEngine.defaultRenderPipeline.DefaultRenderPipeline;
import Kurama.renderingEngine.ginchan.Gintoki;
import Kurama.scene.Scene;
import Kurama.utils.Logger;

import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.*;

public class MindPalace extends Game {

    public Camera playerCamera;
    public boolean isGameRunning = true;

    protected float mouseXSensitivity = 20f;
    protected float mouseYSensitivity = 20f;
    protected float speed = 15f;
    protected float speedMultiplier = 1;
    protected float speedIncreaseMultiplier = 2;

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
        loadScene();

        ((DisplayLWJGL)display).setClearColor(0,0,0,1);
        scene.cameras.forEach(Camera::updateValues);
        targetFPS = display.getRefreshRate();

        Logger.log("");
        for(var mat: scene.materialLibrary) {
            Logger.log(mat.matName);
        }

    }

    public void initGUI() {

        rootGuiComponent = new MasterWindow(this, display, input,"masterWindow");
        var masterWindowConfig = new BoundaryConfigurator() {
            BoundaryConfigurator pre_config = new StretchSystemConfigurator();
            @Override
            public Boundary configure(Boundary boundary) {
                pre_config.configure(boundary);
                boundary.setContainerVisibility(false);
                if(boundary.boundaryOrient == Boundary.BoundaryOrient.Vertical) {
                    boundary.width = 0;
                }
                else {
                    boundary.height = 0;
                }
                return null;
            }
        };

        rootGuiComponent
                .setConfigurator(masterWindowConfig)
                .setColor(new Vector(0,0,0,0f))
                .setContainerVisibility(true);

        var gameScreen =
                new Rectangle(this, rootGuiComponent, "gameScreen")
                        .attachSelfToParent(rootGuiComponent)
                        .setTexture(new Texture(playerCamera.renderBuffer.textureId))
                        .addOnResizeAction(new WidthHeightPercent(1,1))
                        .addOnResizeAction(new ResizeCameraRenderResolution(playerCamera))
                        .setKeyInputFocused(true)
                        .addOnClickAction(new GrabKeyboardFocus())
                        .addOnKeyInputFocusedInitAction((Component current, Input input, float timeDelta) -> {
                            rootGuiComponent.input.disableCursor();
                            isGameRunning = true;

                        })
                        .addOnKeyInputFocusedAction(new MindPalaceMainInputHandling(this))
                        .addOnKeyInputFocusLossInitAction((Component current, Input input, float timeDelta) -> {
                            rootGuiComponent.input.enableCursor();
                            isGameRunning = false;
                        });

    }

    public void loadScene() {
        var hints = new MeshBuilderHints();

        var palace = scene.createModel(scene.loadMesh("res/palace3/3d-model.obj", "palace", hints), "palace",
                Arrays.asList(new String[]{DefaultRenderPipeline.sceneShaderBlockID}));
        palace.meshes.stream().forEach(m -> scene.renderPipeline.initializeMesh(m));
        palace.setScale(0.01f);
        scene.rootSceneComp.addChild(palace);

        var skybox_mesh = scene.loadMesh("res/misc/skybox.obj", "skybox_mesh", hints);
        scene.renderPipeline.initializeMesh(skybox_mesh);
        Material skyMat = new Material(new Texture("res/misc/skybox.png"),1, "SkyBox");
        skyMat.ambientColor = new Vector(new float[]{1f,1f,1f,1f});
        skybox_mesh.materials.set(0, skyMat);

        Model skybox = scene.createModel(skybox_mesh, "skybox", Arrays.asList(new String[]{DefaultRenderPipeline.skyboxShaderBlockID}));
        skybox.setScale(1000);
        scene.skybox = skybox;
        scene.skybox.parent = scene.rootSceneComp;
        scene.rootSceneComp.addChild(scene.skybox);
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
