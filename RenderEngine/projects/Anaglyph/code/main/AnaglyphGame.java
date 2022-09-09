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
import static Kurama.utils.Logger.logError;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.stackPush;

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
            log("successfuly loaded meshes");
        }
        catch (Exception e) {
            logError("Failed to load mesh");
            throw new IllegalArgumentException("Could not load mesh");
        }

        var lostEmpire = new Model(this, meshes, "Lost Empire");
        lostEmpire.setScale(1f);
        lostEmpire.setPos(new Vector(5, -10, 0));

//        List<Mesh> meshes2;
//        try {
//            meshes2 = AssimpStaticLoader.load("projects/VulkanTestProject/models/meshes/house.obj", textureDir);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//
//        //Add texture loc
//        meshes2.get(0).materials.get(0).texture = Texture.createTexture(textureDir + "house_text.jpg");
//
//        //add color just for the sake of consistency with vulkan tutorial
//        var colors2 = new ArrayList<Vector>();
//        meshes2.get(0).getVertices().forEach(v -> colors2.add(new Vector(new float[]{1f,1f,1f})));
//        meshes2.get(0).setAttribute(colors2, Mesh.COLOR);
//        var house = new Model(this, meshes2, "house");
////        house.pos = new Vector(0,0,-2);
//        house.setScale(0.1f);
////        house.setOrientation(Quaternion.getQuaternionFromEuler(-90, 0,0));

        models.add(lostEmpire);
//        models.add(house);

        renderables.addAll(getRenderablesFromModel(lostEmpire));
//        renderables.add(new Renderable(house.meshes.get(0), house));

        renderables.forEach(r -> {
            renderer.uploadRenderable(r);
            deletionQueue.add(() -> r.cleanUp(renderer.vmaAllocator));

            r.mesh.materials.get(0).texture = Texture.createTexture(textureDir + "lost_empire-RGB.png");
            renderer.loadTexture((TextureVK) r.getMaterial().texture);
        });

        var texture = renderer.loadedTextures.get(renderables.get(1).getMaterial().texture.fileName);
        texture.textureSampler = renderer.getTextureSampler(texture.mipLevels);

        try (var stack = stackPush()) {
            renderer.multiViewRenderPass.temp_singleTextureDescriptorSet = renderer.createTextureDescriptorSet(
                    texture.textureSampler,
                    texture.textureImageView,
                    renderer.globalDescriptorPool,
                    0,
                    stack);
        }
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
            renderer.gpuCameraDataLeft.view = playerCamera.leftObjectToWorld;
            renderer.gpuCameraDataLeft.projview = renderer.gpuCameraDataLeft.proj.matMul(renderer.gpuCameraDataLeft.view);

            renderer.gpuCameraDataRight.proj = playerCamera.rightProjection;
            renderer.gpuCameraDataRight.proj.getData()[1][1] *= -1;
            renderer.gpuCameraDataRight.view = playerCamera.rightObjectToWorld;
            renderer.gpuCameraDataRight.projview = renderer.gpuCameraDataRight.proj.matMul(renderer.gpuCameraDataRight.view);

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

        if(input.keyDown(input.W)) {
            float cameraSpeed = this.speed * this.speedMultiplier;
            Vector[] rotationMatrix = this.playerCamera.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector x = rotationMatrix[0];
            Vector y = new Vector(new float[] {0,1,0});
            Vector z = x.cross(y);
            velocity = velocity.add((z.scalarMul(cameraSpeed)));
        }

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

        if(input.keyDown(input.S)) {
            float cameraSpeed = this.speed * this.speedMultiplier;
            Vector[] rotationMatrix = this.playerCamera.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector x = rotationMatrix[0];
            Vector y = new Vector(new float[] {0,1,0});
            Vector z = x.cross(y);
            velocity = velocity.add(z.scalarMul(-cameraSpeed));
        }

        if(input.keyDown(input.A)) {
            float cameraSpeed = this.speed * this.speedMultiplier;
            Vector[] rotationMatrix = this.playerCamera.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector v = rotationMatrix[0];
            velocity = velocity.add(v.scalarMul(cameraSpeed));
        }

        if(input.keyDown(input.D)) {
            float cameraSpeed = this.speed * this.speedMultiplier;
            Vector[] rotationMatrix = this.playerCamera.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector v = rotationMatrix[0];
            velocity = velocity.add(v.scalarMul(-cameraSpeed));
        }

        if(input.keyDown(input.SPACE)) {
            float cameraSpeed = this.speed * this.speedMultiplier;

            Vector v = new Vector(new float[] {0,1,0});
            velocity = velocity.add(v.scalarMul(-cameraSpeed));
        }

        if(input.keyDown(input.LEFT_SHIFT)) {
            float cameraSpeed = this.speed * this.speedMultiplier;

            Vector v = new Vector(new float[] {0,1,0});
            velocity = velocity.add(v.scalarMul(cameraSpeed));
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

            float yawIncrease   = this.mouseXSensitivity * this.timeDelta * this.input.getDelta().get(0);
            float pitchIncrease = this.mouseYSensitivity * this.timeDelta * this.input.getDelta().get(1);

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
