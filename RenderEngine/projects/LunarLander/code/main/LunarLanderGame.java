package main;

import Kurama.ComponentSystem.components.model.Model;
import Kurama.Math.Vector;
import Kurama.Mesh.Mesh;
import Kurama.Mesh.Meshlet;
import Kurama.Mesh.Texture;
import Kurama.Vulkan.TextureVK;
import Kurama.camera.Camera;
import Kurama.display.DisplayVulkan;
import Kurama.game.Game;
import Kurama.geometry.assimp.AssimpStaticLoader;
import Kurama.inputs.InputLWJGL;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static Kurama.Mesh.MeshletGen.generateMeshlets;
import static Kurama.Vulkan.Renderable.getRenderablesFromModel;
import static Kurama.utils.Logger.log;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;

public class LunarLanderGame extends Game {
    public Camera mainCamera;
    public boolean isGameRunning = true;
    public static float gravity = 0.001f;
    public static float winMaxAngle = 5f;
    public static float winMaxSpeed = 0.001f;
    public float thrustAccel = gravity * 1.4f;
    public float turnVel = 20f;
    public List<UFO> ufos = new ArrayList<>();
    public Mesh ufoMesh;

    public List<Model> models = new ArrayList<>();
    public LunarLanderRenderer renderer;

    public float timePerRun = 20f; //seconds
    public int populationSize = 20;
    public int[] layers = new int[]{6, 10, 3};
    public float timeSinceStartedRun = 0f; //seconds
    public int numGenerationsToRun = 100;

    public Vector startingPos = new Vector(0, 0, -20);
    public float yTop = 12f;
    public float yBottom = -10.1f;
    public float xLeft = -22f;
    public float xRight = 22f;

    public LunarLanderGame(String threadName) {
        super(threadName);
        GRAPHICS_API = GraphicsApi.VULKAN;
    }

    @Override
    public void init() {

        renderingEngine = new LunarLanderRenderer(this);
        renderer = (LunarLanderRenderer) renderingEngine;

        display = new DisplayVulkan(this);
        display.resizeEvents.add(() -> renderer.windowResized = true);

        renderingEngine.init(null);
        loadScene();

        this.setTargetFPS(Integer.MAX_VALUE);
        this.shouldDisplayFPS = true;

        this.input = new InputLWJGL(this, (DisplayVulkan) display);

        mainCamera = new Camera(this,null, null,
                new Vector(new float[] {0,0,0}),60,
                0.001f, 1000.0f,
                renderer.swapChainExtent.width(), renderer.swapChainExtent.height(),
                false, "playerCam");

        mainCamera.fovX = 45;
        mainCamera.focalLength = 10;
        mainCamera.nearClippingPlane = mainCamera.focalLength / 5.0f;
        mainCamera.shouldUpdateValues = true;
        renderer.gpuCameraData = new LunarLanderRenderer.GPUCameraData();

        renderer.gpuCameraData.proj = mainCamera.getPerspectiveProjectionMatrix();
        renderer.gpuCameraData.proj.getData()[1][1] *= -1;
        renderer.gpuCameraData.worldToCam = mainCamera.getWorldToObject();
        renderer.gpuCameraData.projWorldToCam = renderer.gpuCameraData.proj.matMul(renderer.gpuCameraData.worldToCam);

        if (mainCamera.shouldUpdateValues) {
            mainCamera.updateValues();
            mainCamera.setShouldUpdateValues(false);
            renderer.cameraUpdatedEvent();
        }

        mainCamera.tick(null, input, timeDelta, false);

        display.resizeEvents.add(() -> {
            mainCamera.renderResolution = display.windowResolution;
            mainCamera.setShouldUpdateValues(true);
        });

        renderer.gpuSceneData = new LunarLanderRenderer.GPUSceneData();
        renderer.gpuSceneData.sunLightColor = new Vector(new float[]{1,1,1,1});
        renderer.gpuSceneData.ambientColor = new Vector(new float[]{1,1,1,1});
        renderer.gpuSceneData.fogDistance = new Vector(new float[]{200,200,0,0});
        renderer.gpuSceneData.sunlightDirection = new Vector(new float[]{0,-1,0,1});
        renderer.gpuSceneData.fogColor = new Vector(new float[]{1,1,1,1});

        display.disableCursor();
    }

    @Override
    public void render() {
        renderer.render();
    }

    public void loadScene() {
        loadUFOMesh();
        createLandingPad();
    }

    public void loadUFOMesh() {
        var location = "projects/LunarLander/models/meshes/cube.obj";
        var textureDir = "projects/LunarLander/models/textures/";

        List<Mesh> meshes;
        List<Meshlet> meshlets = new ArrayList<>();

        try {
            meshes = AssimpStaticLoader.load(location, textureDir);

            // TODO: Temporary because of bug KE:16
            var tex = Texture.createTexture(textureDir + "ufo/ufo.png");
            for(var m: meshes) {
                m.materials.get(0).texture = tex;
                m.boundingRadius = 1;
                log("Creating meshlets");
                var results = generateMeshlets(m, 64);
                log("Finished creating meshlets. Nul of meshlets: " + results.meshlets().size() + " for num of prims: "+ m.indices.size()/3);
                meshlets.addAll(results.meshlets());
            }

            ufoMesh = meshes.get(0);
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Could not load UFO mesh");
        }
    }

    public void instantiateUFOs(int num, Vector startingPos) {
        for(int i = 0; i < num; i++) {
            instantiateUFO(startingPos, "UFO_"+i);
        }
    }

    public UFO instantiateUFO(Vector pos, String id) {
        var ufo = new UFO(this, List.of(new Mesh[]{ufoMesh}), id);
        ufo.setScale(1f);
        ufo.setPos(pos);
        ufo.selfRenderableReference = getRenderablesFromModel(ufo).get(0);

        models.add(ufo);
        ufos.add(ufo);
        renderer.renderables.add(ufo.selfRenderableReference);

        renderer.prepareTexture((TextureVK) ufo.selfRenderableReference.getMaterial().texture);
        ufo.selfRenderableReference.textureDescriptorSet =
                renderer.generateTextureDescriptorSet((TextureVK) ufo.selfRenderableReference.getMaterial().texture);

        return ufo;
    }

    public void createLandingPad() {
        var location = "projects/LunarLander/models/meshes/cube.obj";
        var textureDir = "projects/LunarLander/models/textures/";

        List<Mesh> meshes;
        List<Meshlet> meshlets = new ArrayList<>();

        try {
            meshes = AssimpStaticLoader.load(location, textureDir);

            // TODO: Temporary because of bug KE:16
            var tex = Texture.createTexture(textureDir + "landing.jpg");
            for(var m: meshes) {
                m.materials.get(0).texture = tex;
                m.boundingRadius = 1;
                log("Creating meshlets");
                var results = generateMeshlets(m, 64);
                log("Finished creating meshlets. Nul of meshlets: " + results.meshlets().size() + " for num of prims: "+ m.indices.size()/3);
                meshlets.addAll(results.meshlets());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Could not load mesh");
        }

        var landing = new Model(this, meshes, "UFO");
        landing.setScale(25,1,1);
        landing.setPos(new Vector(-0, -12, -20));
        models.add(landing);
        var landingPadRenderable = getRenderablesFromModel(landing);
        renderer.renderables.addAll(landingPadRenderable);

        landingPadRenderable.forEach(r -> {
            renderer.prepareTexture((TextureVK) r.getMaterial().texture);
            r.textureDescriptorSet = renderer.generateTextureDescriptorSet((TextureVK) r.getMaterial().texture);

            renderer.deletionQueue.add(() -> r.cleanUp(renderer.vmaAllocator));
        });
    }

    @Override
    public void tick() {
        glfwPollEvents();
        input.poll();

        if(input.keyDownOnce(input.ESCAPE)) {
            if(isGameRunning) {
                isGameRunning = false;
                display.enableCursor();
            }
            else {
                isGameRunning = true;
                display.disableCursor();
            }
        }

        if(isGameRunning) {

            performInputHandling(this.timeDelta);
            renderer.multiViewRenderPass.frames.get(renderer.currentMultiViewFrameIndex).shouldUpdateObjectBuffer = true;

            // Should start new gen
            if(ufos.size() == 0) {
                System.out.println("Here");
                instantiateUFOs(populationSize, startingPos);
                renderer.generateMeshBuffers();
            }

            ufos.forEach(ufo -> ufo.process(timeDelta));
            models.forEach(m -> m.tick(null, input, timeDelta, false));


            if(timeSinceStartedRun >= timePerRun) {
                models.removeAll(ufos);
                renderer.renderables.removeAll(ufos.stream().map(u -> u.selfRenderableReference).collect(Collectors.toList()));
                ufos.clear();
                timeSinceStartedRun = 0;
            }else {
                timeSinceStartedRun += timeDelta;
            }

        }

        if(glfwWindowShouldClose(((DisplayVulkan)display).window)) {
            programRunning = false;
            isGameRunning = false;
        }
        input.reset();
    }

    public void performInputHandling(float timeDelta) {

        if(input.keyDown(input.F)) {
            display.toggleWindowModes();
        }
    }

    @Override
    public void cleanUp() {
        renderingEngine.cleanUp();
        display.cleanUp();
    }

    @Override
    public String getDisplayFPS() {
        // 2 to account for the two images rendered and displayed during each "frame"
        return String.valueOf((int)fps);
    }

}
