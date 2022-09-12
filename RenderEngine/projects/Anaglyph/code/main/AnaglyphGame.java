package main;

import Kurama.ComponentSystem.components.model.Model;
import Kurama.Math.Quaternion;
import Kurama.Math.Vector;
import Kurama.Mesh.Mesh;
import Kurama.Mesh.Texture;
import Kurama.Vulkan.*;
import Kurama.camera.StereoCamera;
import Kurama.display.DisplayVulkan;
import Kurama.game.Game;
import Kurama.geometry.assimp.AssimpStaticLoader;
import Kurama.inputs.InputLWJGL;

import java.util.ArrayList;
import java.util.List;

import static Kurama.Vulkan.Renderable.getRenderablesFromModel;
import static Kurama.Vulkan.VulkanUtilities.deletionQueue;
import static Kurama.utils.Logger.log;
import static org.lwjgl.glfw.GLFW.*;

public class AnaglyphGame extends Game {

    public DisplayVulkan display;
    public StereoCamera playerCamera;
    public float mouseXSensitivity = 20f;
    public float mouseYSensitivity = 20f;
    public float speed = 15f;
    public float speedMultiplier = 1;
    public float speedIncreaseMultiplier = 2;
    public boolean isGameRunning = true;

    public List<Model> models = new ArrayList<>();
    public List<Renderable> renderables = new ArrayList<>();
    public AnaglyphRenderer renderer;

    public AnaglyphGame(String threadName) {
        super(threadName);
        GRAPHICS_API = GraphicsApi.VULKAN;
    }

    @Override
    public void init() {

        renderingEngine = new AnaglyphRenderer(this);
        renderer = (AnaglyphRenderer) renderingEngine;

        display = new DisplayVulkan(this);
        display.resizeEvents.add(() -> renderer.framebufferResize = true);

        renderingEngine.init(null);
        loadScene();

        this.setTargetFPS(Integer.MAX_VALUE);
        this.shouldDisplayFPS = true;

        this.input = new InputLWJGL(this, display);

        playerCamera = new StereoCamera(this,null, null, new Vector(new float[] {0,0,0}),60, 0.001f, 1000.0f,
                renderer.swapChainExtent.width(), renderer.swapChainExtent.height(), false, "playerCam");

        playerCamera.loadDefaultSettings();
        playerCamera.fovX = 45;
        playerCamera.focalLength = 10;
        playerCamera.eyeSeparation = playerCamera.focalLength / 30f;
        playerCamera.nearClippingPlane = playerCamera.focalLength / 5.0f;

        playerCamera.shouldUpdateValues = true;

        display.resizeEvents.add(() -> {
            playerCamera.renderResolution = display.windowResolution;
            playerCamera.setShouldUpdateValues(true);
        });

        renderer.gpuCameraDataLeft = new AnaglyphRenderer.GPUCameraData();
        renderer.gpuCameraDataRight = new AnaglyphRenderer.GPUCameraData();

        renderer.gpuSceneData = new AnaglyphRenderer.GPUSceneData();
        renderer.gpuSceneData.sunLightColor = new Vector(new float[]{1,1,1,1});
        renderer.gpuSceneData.ambientColor = new Vector(new float[]{1,1,1,1});
        renderer.gpuSceneData.fogDistance = new Vector(new float[]{200,200,0,0});
        renderer.gpuSceneData.sunlightDirection = new Vector(new float[]{0,-1,0,1});
        renderer.gpuSceneData.fogColor = new Vector(new float[]{1,1,1,1});

        display.disableCursor();
    }

    @Override
    public void render() {
        renderer.render(renderables);
    }

    public void loadScene() {

        var location = "projects/Anaglyph/models/meshes/lost_empire.obj";
        var textureDir = "projects/Anaglyph/models/textures/";

        List<Mesh> meshes;

        try {
            meshes = AssimpStaticLoader.load(location, textureDir);

            // TODO: Temporary because of bug KE:16
            var tex = Texture.createTexture(textureDir + "lost_empire-RGB.png");
            for(var m: meshes) {
                m.materials.get(0).texture = tex;
            }
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Could not load mesh");
        }

        var lostEmpire = new Model(this, meshes, "Lost Empire");
        lostEmpire.setScale(1f);
        lostEmpire.setPos(new Vector(5, -10, 0));

        List<Mesh> meshes2;
        try {
            meshes2 = AssimpStaticLoader.load("projects/VulkanTestProject/models/meshes/viking_room.obj", textureDir);

            // TODO: Temporary because of bug KE:16
            var tex = Texture.createTexture(textureDir + "viking_room.png");
            for(var m: meshes2) {
                m.materials.get(0).texture = tex;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        //Add texture loc
        var vikingRoom = new Model(this, meshes2, "vikingRoom");
        vikingRoom.orientation = Quaternion.getQuaternionFromEuler(-90, 0, 0);
        vikingRoom.setPos(new Vector(0, 50, 0));
        vikingRoom.setScale(10);

        models.add(lostEmpire);
        models.add(vikingRoom);

        renderables.addAll(getRenderablesFromModel(lostEmpire));
        renderables.add(new Renderable(vikingRoom.meshes.get(0), vikingRoom));

        renderables.forEach(r -> {
            renderer.uploadMeshData(r);
            renderer.prepareTexture((TextureVK) r.getMaterial().texture);
            r.textureDescriptorSet = renderer.generateTextureDescriptorSet((TextureVK) r.getMaterial().texture);

            deletionQueue.add(() -> r.cleanUp(renderer.vmaAllocator));
        });
    }

    @Override
    public void tick() {
        glfwPollEvents();
        input.poll();

        cameraUpdates(this.timeDelta);

        if(isGameRunning) {

            // Camera updates
            playerCamera.velocity = playerCamera.velocity.add(playerCamera.acceleration.scalarMul(timeDelta));
            var detlaV = playerCamera.velocity.scalarMul(timeDelta);
            playerCamera.setPos(playerCamera.getPos().add(detlaV));

            if (playerCamera.shouldUpdateValues) {
                playerCamera.updateValues();
                playerCamera.setShouldUpdateValues(false);
            }

            renderer.gpuCameraDataLeft.proj = playerCamera.leftProjection;
            renderer.gpuCameraDataLeft.proj.getData()[1][1] *= -1;
            renderer.gpuCameraDataLeft.worldToCam = playerCamera.leftWorldToCam;
            renderer.gpuCameraDataLeft.projWorldToCam = renderer.gpuCameraDataLeft.proj.matMul(renderer.gpuCameraDataLeft.worldToCam);

            renderer.gpuCameraDataRight.proj = playerCamera.rightProjection;
            renderer.gpuCameraDataRight.proj.getData()[1][1] *= -1;
            renderer.gpuCameraDataRight.worldToCam = playerCamera.rightWorldToCam;
            renderer.gpuCameraDataRight.projWorldToCam = renderer.gpuCameraDataRight.proj.matMul(renderer.gpuCameraDataRight.worldToCam);

            playerCamera.tick(null, input, timeDelta, false);

            // Call tick on all models
            models.forEach(m -> m.tick(null, input, timeDelta, false));
        }

        if(glfwWindowShouldClose(display.window)) {
            programRunning = false;
            isGameRunning = false;
        }
        input.reset();
    }

    public void cameraUpdates(float timeDelta) {
        Vector velocity = new Vector(3,0);

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

        if(input.keyDown(input.W)) {
            float cameraSpeed = this.speed * this.speedMultiplier;
            Vector[] rotationMatrix = this.playerCamera.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector x = rotationMatrix[0];
            Vector y = new Vector(new float[] {0,1,0});
            Vector z = x.cross(y);
            velocity = velocity.add((z.scalarMul(-cameraSpeed)));
        }

        if(input.keyDown(input.S)) {
            float cameraSpeed = this.speed * this.speedMultiplier;
            Vector[] rotationMatrix = this.playerCamera.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector x = rotationMatrix[0];
            Vector y = new Vector(new float[] {0,1,0});
            Vector z = x.cross(y);
            velocity = velocity.add(z.scalarMul(cameraSpeed));
        }

        if(input.keyDown(input.A)) {
            float cameraSpeed = this.speed * this.speedMultiplier;
            Vector[] rotationMatrix = this.playerCamera.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector v = rotationMatrix[0];
            velocity = velocity.add(v.scalarMul(-cameraSpeed));
        }

        if(input.keyDown(input.D)) {
            float cameraSpeed = this.speed * this.speedMultiplier;
            Vector[] rotationMatrix = this.playerCamera.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector v = rotationMatrix[0];
            velocity = velocity.add(v.scalarMul(cameraSpeed));
        }

        if(input.keyDown(input.F)) {
            display.toggleWindowModes();
        }

        if(input.keyDown(input.SPACE)) {
            float cameraSpeed = this.speed * this.speedMultiplier;

            Vector v = new Vector(new float[] {0,1,0});
            velocity = velocity.add(v.scalarMul(cameraSpeed));
        }

        if(input.keyDown(input.LEFT_SHIFT)) {
            float cameraSpeed = this.speed * this.speedMultiplier;

            Vector v = new Vector(new float[] {0,1,0});
            velocity = velocity.add(v.scalarMul(-cameraSpeed));
        }

        if(input.keyDownOnce(input.LEFT_CONTROL)) {
            if(this.speedMultiplier == 1) this.speedMultiplier = this.speedIncreaseMultiplier;
            else this.speedMultiplier = 1;
        }

        if(input.keyDown(input.LEFT_ARROW)) {
            models.get(0).orientation = models.get(0).orientation.multiply(Quaternion.getQuaternionFromEuler(0, 0, -90 * timeDelta));
        }

        if(input.keyDown(input.RIGHT_ARROW)) {
            models.get(0).orientation = models.get(0).orientation.multiply(Quaternion.getQuaternionFromEuler(0, 0, 90 * timeDelta));
        }

        this.playerCamera.velocity = velocity;
        calculate3DCamMovement();

        this.playerCamera.setShouldUpdateValues(true);
    }

    private void calculate3DCamMovement() {
        if (this.input.getDelta().getNorm() != 0 && this.isGameRunning) {

            float yawIncrease   = this.mouseXSensitivity * -this.timeDelta * this.input.getDelta().get(0);
            float pitchIncrease = this.mouseYSensitivity * -this.timeDelta * this.input.getDelta().get(1);

            Vector currentAngle = this.playerCamera.getOrientation().getPitchYawRoll();
            float currentPitch = currentAngle.get(0) + pitchIncrease;

            if(currentPitch >= 0 && currentPitch > 60) {
                pitchIncrease = 0;
            }
            else if(currentPitch < 0 && currentPitch < -60) {
                pitchIncrease = 0;
            }

            Quaternion pitch = Quaternion.getAxisAsQuat(new Vector(new float[] {1,0,0}),pitchIncrease);
            Quaternion yaw = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}),yawIncrease);

            Quaternion q = this.playerCamera.getOrientation();

            q = q.multiply(pitch);
            q = yaw.multiply(q);
            this.playerCamera.setOrientation(q);
        }
    }

    @Override
    public void cleanUp() {
        renderingEngine.cleanUp();
        display.cleanUp();
    }

}