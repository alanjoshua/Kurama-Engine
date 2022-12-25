package main;

import Kurama.ComponentSystem.components.model.Model;
import Kurama.Math.Quaternion;
import Kurama.Math.Vector;
import Kurama.Mesh.Mesh;
import Kurama.Mesh.Texture;
import Kurama.Vulkan.Renderable;
import Kurama.Vulkan.TextureVK;
import Kurama.camera.Camera;
import Kurama.display.DisplayVulkan;
import Kurama.game.Game;
import Kurama.geometry.assimp.AssimpStaticLoader;
import Kurama.inputs.InputLWJGL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static Kurama.Mesh.MeshletGen.generateMeshlets;
import static Kurama.Vulkan.Renderable.getRenderablesFromModel;
import static Kurama.utils.Logger.log;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;

public class PointCloudController extends Game {

    public PointCloudRenderer renderer;
    public float mouseXSensitivity = 20f;
    public float mouseYSensitivity = 20f;
    public float speed = 15f;
    public float speedMultiplier = 1;
    public float speedIncreaseMultiplier = 2;
    public boolean isGameRunning = true;
    public List<Mesh.VERTATTRIB> meshAttribsToLoad = new ArrayList<>(Arrays.asList(Mesh.VERTATTRIB.POSITION, Mesh.VERTATTRIB.COLOR));

    public List<Model> models = new ArrayList<>();
    public Camera mainCamera;

    public PointCloudController(String name) {
        super(name);
    }

    @Override
    public void init() {
        renderingEngine = new PointCloudRenderer(this);
        renderer = (PointCloudRenderer) renderingEngine;

        display = new DisplayVulkan(this);
        display.resizeEvents.add(() -> renderer.windowResized = true);

        renderingEngine.init(null);

        for(var key: meshAttribsToLoad) {
            renderer.globalVertAttribs.put(key, new ArrayList<>());
        }
        loadScene();

        this.setTargetFPS(Integer.MAX_VALUE);
        this.shouldDisplayFPS = true;

        this.input = new InputLWJGL(this, (DisplayVulkan) display);

        mainCamera = new Camera(this,null, null,
                new Vector(new float[] {0,0,0}),60,
                0.001f, 1000.0f,
                renderer.swapChainExtent.width(), renderer.swapChainExtent.height(),
                false, "playerCam");

        mainCamera.shouldUpdateValues = true;

        display.resizeEvents.add(() -> {
            mainCamera.renderResolution = display.windowResolution;
            mainCamera.setShouldUpdateValues(true);
        });
        display.disableCursor();
    }

    @Override
    public void cleanUp() {
        renderingEngine.cleanUp();
        display.cleanUp();
    }

    @Override
    public void tick() {
        glfwPollEvents();
        input.poll();

        cameraUpdates(this.timeDelta);

        if(isGameRunning) {

            // Camera updates
            mainCamera.velocity = mainCamera.velocity.add(mainCamera.acceleration.scalarMul(timeDelta));
            var detlaV = mainCamera.velocity.scalarMul(timeDelta);
            mainCamera.setPos(mainCamera.getPos().add(detlaV));

            if (mainCamera.shouldUpdateValues) {
                mainCamera.updateValues();
                mainCamera.setShouldUpdateValues(false);
                renderer.cameraUpdatedEvent();
            }

            mainCamera.tick(null, input, timeDelta, false);
            // Call tick on all models
            models.forEach(m -> m.tick(null, input, timeDelta, false));

        }

        if(glfwWindowShouldClose(((DisplayVulkan)display).window)) {
            programRunning = false;
            isGameRunning = false;
        }
        input.reset();
    }

    public void loadScene() {

        var location = "projects/ActiveShutter/models/meshes/lost_empire.obj";
        var textureDir = "projects/ActiveShutter/models/textures/";

        List<Mesh> meshes;

        try {
            meshes = AssimpStaticLoader.load(location, textureDir);

            // TODO: Temporary because of bug KE:16
            var tex = Texture.createTexture(textureDir + "lost_empire-RGB.png");
            for(var m: meshes) {
                m.materials.get(0).texture = tex;
                m.boundingRadius = 50;

                log("Creating meshlets");
                var results = generateMeshlets(m, 3, 64, 124,
                        renderer.globalVertAttribs.size(), renderer.meshletVertexIndexBuffer.size(), renderer.meshletLocalIndexBuffer.size());
                log("Finished creating meshlets. Nul of meshlets: " + results.meshlets().size() + " for num of prims: "+ m.indices.size()/3);

                results.meshlets().forEach(meshlet -> meshlet.objectId = 0);
                renderer.meshlets.addAll(results.meshlets());
                for(var key: meshAttribsToLoad) {
                    if(!m.vertAttributes.containsKey(key)) {
                        throw new RuntimeException("Mesh "+ m.meshLocation + " does not have the required vertex attribute: "+ key);
                    }
                    renderer.globalVertAttribs.get(key).addAll(m.vertAttributes.get(key));
                }
                renderer.meshletVertexIndexBuffer.addAll(results.vertexIndexBuffer());
                renderer.meshletLocalIndexBuffer.addAll(results.localIndexBuffer());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
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

                log("Creating meshlets");
                var results = generateMeshlets(m, 3, 64, 124,
                        renderer.globalVertAttribs.size(), renderer.meshletVertexIndexBuffer.size(), renderer.meshletLocalIndexBuffer.size());
                log("Finished creating meshlets. Nul of meshlets: "+ results.meshlets().size() + " for num of prims: "+ m.indices.size()/3);

                results.meshlets().forEach(meshlet -> meshlet.objectId = 1);
                renderer.meshlets.addAll(results.meshlets());
                for(var key: meshAttribsToLoad) {
                    if(!m.vertAttributes.containsKey(key)) {
                        throw new RuntimeException("Mesh "+ m.meshLocation + " does not have the required vertex attribute: "+ key);
                    }
                    renderer.globalVertAttribs.get(key).addAll(m.vertAttributes.get(key));
                }
                renderer.meshletVertexIndexBuffer.addAll(results.vertexIndexBuffer());
                renderer.meshletLocalIndexBuffer.addAll(results.localIndexBuffer());
            }

            log("total num of meshlets in this scene = "+ renderer.meshlets.size());
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

        renderer.renderables.addAll(getRenderablesFromModel(lostEmpire));
        renderer.renderables.add(new Renderable(vikingRoom.meshes.get(0), vikingRoom));

        renderer.renderables.forEach(r -> {
            renderer.prepareTexture((TextureVK) r.getMaterial().texture);
            r.textureDescriptorSet = renderer.generateTextureDescriptorSet((TextureVK) r.getMaterial().texture);

            renderer.deletionQueue.add(() -> r.cleanUp(renderer.vmaAllocator));
        });

        renderer.generateMeshBuffers();
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
            Vector[] rotationMatrix = this.mainCamera.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector x = rotationMatrix[0];
            Vector y = new Vector(new float[] {0,1,0});
            Vector z = x.cross(y);
            velocity = velocity.add((z.scalarMul(-cameraSpeed)));
        }

        if(input.keyDown(input.S)) {
            float cameraSpeed = this.speed * this.speedMultiplier;
            Vector[] rotationMatrix = this.mainCamera.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector x = rotationMatrix[0];
            Vector y = new Vector(new float[] {0,1,0});
            Vector z = x.cross(y);
            velocity = velocity.add(z.scalarMul(cameraSpeed));
        }

        if(input.keyDown(input.A)) {
            float cameraSpeed = this.speed * this.speedMultiplier;
            Vector[] rotationMatrix = this.mainCamera.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector v = rotationMatrix[0];
            velocity = velocity.add(v.scalarMul(-cameraSpeed));
        }

        if(input.keyDown(input.D)) {
            float cameraSpeed = this.speed * this.speedMultiplier;
            Vector[] rotationMatrix = this.mainCamera.getOrientation().getRotationMatrix().convertToColumnVectorArray();

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

        this.mainCamera.velocity = velocity;
        calculate3DCamMovement();

        this.mainCamera.setShouldUpdateValues(true);
    }

    private void calculate3DCamMovement() {
        if (this.input.getDelta().getNorm() != 0 && this.isGameRunning) {

            float yawIncrease   = this.mouseXSensitivity * -this.timeDelta * this.input.getDelta().get(0);
            float pitchIncrease = this.mouseYSensitivity * this.timeDelta * this.input.getDelta().get(1);

            Vector currentAngle = this.mainCamera.getOrientation().getPitchYawRoll();
            float currentPitch = currentAngle.get(0) + pitchIncrease;

            if(currentPitch >= 0 && currentPitch > 60) {
                pitchIncrease = 0;
            }
            else if(currentPitch < 0 && currentPitch < -60) {
                pitchIncrease = 0;
            }

            Quaternion pitch = Quaternion.getAxisAsQuat(new Vector(new float[] {1,0,0}),pitchIncrease);
            Quaternion yaw = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}),yawIncrease);

            Quaternion q = this.mainCamera.getOrientation();

            q = q.multiply(pitch);
            q = yaw.multiply(q);
            this.mainCamera.setOrientation(q);
        }
    }

    @Override
    public void render() {
        renderer.render();
    }
}
