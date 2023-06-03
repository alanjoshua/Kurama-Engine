package main;

import Kurama.ComponentSystem.components.model.Model;
import Kurama.ComponentSystem.components.model.PointCloud;
import Kurama.Math.Quaternion;
import Kurama.Math.Vector;
import Kurama.Mesh.Mesh;
import Kurama.Mesh.Texture;
import Kurama.camera.Camera;
import Kurama.display.DisplayVulkan;
import Kurama.game.Game;
import Kurama.geometry.assimp.AssimpStaticLoader;
import Kurama.inputs.InputLWJGL;
import com.github.mreutegg.laszip4j.LASReader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

import static Kurama.IOps.PointCloudIO.loadPointCloud;
import static Kurama.IOps.PointCloudIO.writePointCloudToFile;
import static Kurama.Mesh.MeshletGen.*;
import static Kurama.Mesh.MeshletGen.MeshletColorMode.*;
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

        loadScene();

        this.setTargetFPS(Integer.MAX_VALUE);
        this.shouldDisplayFPS = true;

        this.input = new InputLWJGL(this, (DisplayVulkan) display);

        mainCamera = new Camera(this,null, null,
                new Vector(new float[] {0,0,0}),60,
                0.1f, 50000.0f,
                renderer.swapChainExtent.width(), renderer.swapChainExtent.height(),
                false, "playerCam");

//        mainCamera.pos = new Vector(-199748,-622242,31061);
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
            renderer.models.forEach(m -> m.tick(null, input, timeDelta, false));
            renderer.tick();
        }
        else {
            renderer.tick();
        }

        if(glfwWindowShouldClose(((DisplayVulkan)display).window)) {
            programRunning = false;
            isGameRunning = false;
        }
        input.reset();
    }

    public void loadScene() {

//        createMinecraftWorld();
//        createRoom();


//        Vector tl = new Vector(627772.4f, 1013350.6f);
//        Vector tr = new Vector(627906.9f, 1013343.1f);
//        Vector br = new Vector(627918.4f, 1013175.5f);
//        Vector bl = new Vector(627739.8f, 1013185.0f);

//        createHead();
//        loadLidar(10000000, tl, tr, br, bl);
//        renderer.createMeshletsAndSyncGeoData( 64);

//        setMeshletColors(PerMeshlet, renderer.meshlets,renderer.globalVertAttribs);

        var head = new PointCloud(this, new Mesh(null, null, null, null, "projects/PointCloud/models/test.pc", null), "head");
        head.setScale(10);
        renderer.addModel(head);

        loadPointCloud("projects/PointCloud/models/test.pc", renderer);

        renderer.models.forEach(m -> m.tick(null, input, timeDelta, false));
        renderer.curFrameMeshletsDrawIndices = new ArrayList<>(IntStream.rangeClosed(0, 0).boxed().toList());
//        renderer.curFrameMeshletsDrawIndices.forEach(i -> renderer.meshlets.get(i).isRendered = true);

        renderer.geometryUpdatedEvent();

//           createMinecraftWorld();

        log("vertex buffer count = " +renderer.globalVertAttribs.get(Mesh.VERTATTRIB.POSITION).size());

        log(" num of total colors: "+ renderer.globalVertAttribs.get(Mesh.VERTATTRIB.COLOR).size());
        log("total num of meshlets: "+renderer.meshlets.size());


//        writePointCloudToFile("projects/PointCloud/models/test.pc",
//                    (PointCloud) renderer.models.get(0), renderer.globalVertAttribs, renderer.meshletToIndexMapping);

    }

    public void loadLidar(int numVerticesToLoad, Vector tl ,Vector tr, Vector br, Vector bl) {
        log("Loading LAZ file: ");
        var lasReader = new LASReader(new File("E:\\rich-lidar\\2022-09-14 lidar one\\YS-20220914-134052-20221122-130404.copc.laz"));
//        var lasReader = new LASReader(new File("E:\\rich-lidar\\2022-09-15 lidar two\\YS-20220915-132041-20221122-144832.copc.laz"));

        List<Vector> lidarPoints;

        if(numVerticesToLoad < 2147483647) { // Java array max size
            lidarPoints = new ArrayList<>(numVerticesToLoad);
        }
        else {
            lidarPoints = new ArrayList<>(2147483647);
        }
        log("Total lidar points: "+lasReader.getHeader().getNumberOfPointRecords());

        var offset = new Vector((float) lasReader.getHeader().getXOffset(),
                (float) lasReader.getHeader().getYOffset(),
                (float) lasReader.getHeader().getZOffset());

        var scale = new Vector((float) lasReader.getHeader().getXScaleFactor(),
                (float)lasReader.getHeader().getYScaleFactor(),
                (float)lasReader.getHeader().getZScaleFactor());
        log("scale: "+scale);
        log("offset: "+offset);


        log("min-max x: "+lasReader.getHeader().getMinX()+":"+lasReader.getHeader().getMaxX() +" y: " +
                lasReader.getHeader().getMinY()+":"+lasReader.getHeader().getMaxY() +" z: " +
                lasReader.getHeader().getMinZ()+":"+lasReader.getHeader().getMaxZ());

//        var offsetvec2 = offset.removeDimensionFromVec(2);
//        tl = tl.sub(offsetvec2);
//        tr = tr.sub(offsetvec2);
//        br = br.sub(offsetvec2);
//        bl = bl.sub(offsetvec2);

        float xmin = Math.min(tl.get(0), bl.get(0));
        float xmax = Math.max(tr.get(0), br.get(0));
        float ymin = Math.min(bl.get(1), br.get(1));
        float ymax = Math.max(tl.get(1), tr.get(1));

        log("gathering points from range: x: "+ xmin + " - "+xmax + " y: "+ ymin + " - " + ymax);

        int counter = 0;
        var avgPos = new Vector(0,0,0);
        try {
            for (var p : lasReader.getPoints()) {

//                var pos = new Vector(p.getX(), p.getY(), p.getZ()).add(new Vector((float) lasReader.getHeader().getMinX(), (float) lasReader.getHeader().getMinY(), (float) lasReader.getHeader().getMinZ()));
//                var pos = new Vector(p.getX(), p.getY(), p.getZ());
                var pos = new Vector(p.getX(), p.getY(), p.getZ()).sub(new Vector(-132932.45f, -596958.6f, 27277.133f));

                avgPos = avgPos.add(pos);
//                if(pos.get(0) >= xmin && pos.get(0) <= xmax && pos.get(1) >= ymin && pos.get(1) <= ymax) {
//                    log("accepted "+pos);
                    lidarPoints.add(pos);
                    counter++;

                    if (counter >= numVerticesToLoad) {
                        lidarPoints.get(0).display();
                        break;
                    }
//                }
            }
            avgPos = avgPos.scalarMul(1f/(float)lidarPoints.size());
            log("avg pos: "+avgPos);
        }
        catch (Exception e) {
            log("crashing after loading these many points: "+lidarPoints.size());
            throw new RuntimeException("Ran outof memory");
        }
        log("Num of lidar points loaded: "+lidarPoints.size());

        var vertAttribs = new HashMap<Mesh.VERTATTRIB, List<Vector>>();
        vertAttribs.put(Mesh.VERTATTRIB.POSITION, lidarPoints);

        var lidarMesh = new Mesh(null, null, vertAttribs, null, "lidar", null);
        lidarMesh.boundingRadius = 100000;

        var lidarModel = new PointCloud(this, lidarMesh, "lidarPointCloud");
        lidarModel.setScale(scale);
//        lidarModel.setPos(avgPos.scalarMul(1));
        lidarModel.orientation = Quaternion.getQuaternionFromEuler(-90, 0, 0);
        renderer.addModel(lidarModel);
    }

    public void createRoom() {
        var textureDir = "projects/ActiveShutter/models/textures/";

        List<Mesh> meshes;
        try {//projects/VulkanTestProject/models/meshes/viking_room.obj
            meshes = AssimpStaticLoader.load("projects/VulkanTestProject/models/meshes/viking_room.obj", textureDir);

            // TODO: Temporary because of bug KE:16
            var tex = Texture.createTexture(textureDir + "viking_room.png");

            var mergedMesh = mergeMeshes(meshes);
            var indices = new ArrayList<Integer>();;
            for(int i = 0; i < mergedMesh.getVertices().size(); i++) {
                indices.add(i);
            }
            mergedMesh.indices = indices;
            mergedMesh.materials.get(0).texture = tex;
            mergedMesh.boundingRadius = 50;
            meshes.clear();
            meshes.add(mergedMesh);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        //Add texture loc
        var vikingRoom = new Model(this, meshes, "vikingRoom");
        vikingRoom.orientation = Quaternion.getQuaternionFromEuler(-90, 0, 0);
        vikingRoom.setPos(new Vector(0, 50, 0));
        vikingRoom.setScale(10);

        renderer.addModel(vikingRoom);
    }

    public void createHead() {
        var textureDir = "projects/ActiveShutter/models/textures/";

        List<Mesh> meshes;
        try {//projects/VulkanTestProject/models/meshes/viking_room.obj
            meshes = AssimpStaticLoader.load("projects/VulkanTestProject/models/meshes/head_decimated.obj", textureDir);

            // TODO: Temporary because of bug KE:16
//            var tex = Texture.createTexture(textureDir + "viking_room.png");

            var mergedMesh = mergeMeshes(meshes);
            var indices = new ArrayList<Integer>();;
            for(int i = 0; i < mergedMesh.getVertices().size(); i++) {
                indices.add(i);
            }
            mergedMesh.indices = indices;
//            mergedMesh.materials.get(0).texture = tex;
            mergedMesh.boundingRadius = 50;
            meshes.clear();
            meshes.add(mergedMesh);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        //Add texture loc
        var head = new PointCloud(this, meshes, "head");
//        vikingRoom.orientation = Quaternion.getQuaternionFromEuler(-90, 0, 0);
//        vikingRoom.setPos(new Vector(0, 50, 0));
        head.setScale(10);

        renderer.addModel(head);
    }

    public void createMinecraftWorld() {
        var location = "projects/ActiveShutter/models/meshes/lost_empire.obj";
        var textureDir = "projects/ActiveShutter/models/textures/";

        List<Mesh> meshes;

        try {
            meshes = AssimpStaticLoader.load(location, textureDir);

            // TODO: Temporary because of bug KE:16
            var tex = Texture.createTexture(textureDir + "lost_empire-RGB.png");

            var mergedMesh = mergeMeshes(meshes);
            var indices = new ArrayList<Integer>();;
            for(int i = 0; i < mergedMesh.getVertices().size(); i++) {
                indices.add(i);
            }
            mergedMesh.indices = indices;

            mergedMesh.materials.get(0).texture = tex;
            mergedMesh.boundingRadius = 50;
            meshes.clear();
            meshes.add(mergedMesh);

        }
        catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Could not load mesh");
        }
//
        var lostEmpire = new Model(this, meshes, "Lost Empire");
        lostEmpire.setScale(1f);
        lostEmpire.setPos(new Vector(5, -10, 0));

        renderer.addModel(lostEmpire);
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

        if(input.keyDown(input.C)) {
            mainCamera.pos = new Vector(0,0,0);
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

        if(input.keyDownOnce(input.F)) {
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

        this.mainCamera.velocity = velocity;
        calculate3DCamMovement();

        this.mainCamera.setShouldUpdateValues(true);
    }

    private void calculate3DCamMovement() {
        if (this.input.getDelta().getNorm() != 0 && this.isGameRunning) {

            float yawIncrease   = this.mouseXSensitivity * this.input.getDelta().get(0) * -this.timeDelta;
            float pitchIncrease = this.mouseYSensitivity * this.input.getDelta().get(1) * -this.timeDelta;

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
//        System.exit(1);
    }
}
