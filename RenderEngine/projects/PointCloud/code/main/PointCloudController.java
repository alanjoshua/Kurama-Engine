package main;

import Kurama.ComponentSystem.components.model.Model;
import Kurama.ComponentSystem.components.model.PointCloud;
import Kurama.Math.Quaternion;
import Kurama.Math.Vector;
import Kurama.Mesh.Mesh;
import Kurama.Mesh.MeshletGen;
import Kurama.Mesh.Texture;
import Kurama.camera.Camera;
import Kurama.display.DisplayVulkan;
import Kurama.game.Game;
import Kurama.geometry.assimp.AssimpStaticLoader;
import Kurama.inputs.InputLWJGL;
import com.github.mreutegg.laszip4j.LASReader;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

import static Kurama.Mesh.MeshletGen.mergeMeshes;
import static Kurama.Mesh.MeshletGen.setMeshletColors;
import static Kurama.utils.Logger.log;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;

public class PointCloudController extends Game {

    public PointCloudRenderer renderer;
    public float mouseXSensitivity = 100f;
    public float mouseYSensitivity = 100f;
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
//
////        createHead();
//        loadLidar(100000, tl, tr, br, bl);
//        setMeshletColors(PerMeshlet, renderer.meshlets,renderer.globalVertAttribs);

//        var head = new PointCloud(this, new Mesh(null, null, null, null, "projects/PointCloud/models/test.pc", null), "head");
//        head.setScale(10);
//        renderer.addModel(head);
//
//        var head = loadPointCloud("projects/PointCloud/models/test.pc", renderer, "head");
//        head.setScale(10);


        var hand = loadHand();
        renderer.addModel(hand);
//        hand.setScale(new Vector(10,10,10));

        renderer.models.forEach(m -> m.tick(null, input, timeDelta, false));
        renderer.curFrameMeshletsDrawIndices = new ArrayList<>(IntStream.rangeClosed(0, 0).boxed().toList());
//        renderer.curFrameMeshletsDrawIndices.forEach(i -> renderer.meshlets.get(i).isRendered = true);
//        renderer.

        renderer.createMeshletsAndSyncGeoData( 64);
        setMeshletColors(MeshletGen.MeshletColorMode.PerMeshlet, renderer.meshlets, renderer.globalVertAttribs);

        renderer.geometryUpdatedEvent();

//           createMinecraftWorld();

        log("vertex buffer count = " +renderer.globalVertAttribs.get(Mesh.VERTATTRIB.POSITION).size());

//        log(" num of total colors: "+ renderer.globalVertAttribs.get(Mesh.VERTATTRIB.COLOR).size());
        log("total num of meshlets: "+renderer.meshlets.size());


//        writePointCloudToFile("projects/PointCloud/models/test.pc",
//                    (PointCloud) renderer.models.get(0), renderer.globalVertAttribs, renderer.meshletToIndexMapping);

    }

    public void loadLidar(int numVerticesToLoad, Vector tl ,Vector tr, Vector br, Vector bl) {
        log("Loading LAZ file: ");
        var lasReader = new LASReader(new File("C:\\Users\\alank\\Documents\\Lidar\\2022-09-14 lidar one\\YS-20220914-134052-20221122-130404.copc.laz"));
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

        var lidarModel = new PointCloud(this, lidarMesh, "lidarPointCloud", 64);
        lidarModel.setScale(scale);
//        lidarModel.setPos(avgPos.scalarMul(1));
        lidarModel.orientation = Quaternion.getQuaternionFromEuler(-90, 0, 0);
        renderer.addModel(lidarModel);
    }

    public PointCloud loadHand() {
        var threshold = 0.3f;

        // Works fine (left hand)
//        var data = "[87.0, -81.25, -166.0], [87.0, -75.0, -156.0], [97.0, -62.5, -166.0], [107.0, -31.25, -186.0], [97.0, -93.75, -166.0], [77.0, -68.75, -166.0], [87.0, -43.75, -166.0], [77.0, -43.75, -166.0], [117.0, -75.0, -176.0], [87.0, -75.0, -176.0], [87.0, -87.5, -166.0], [107.0, 31.25, -186.0], [97.0, -62.5, -166.0], [87.0, -68.75, -176.0], [87.0, -75.0, -186.0], [117.0, -31.25, -176.0], [97.0, -87.5, -166.0], [87.0, -50.0, -166.0], [97.0, -18.75, -176.0], [117.0, 18.75, -176.0], [357.0, 0.0, -246.0], [77.0, -78.58428955078125, -126.0], [97.0, -84.83428955078125, -136.0], [137.0, -78.58428955078125, -136.0], [167.0, -78.58428955078125, -166.0], [57.0, -84.83428955078125, -126.0], [57.0, -97.33428955078125, -136.0], [77.0, -116.08428955078125, -136.0], [127.0, -128.58428955078125, -146.0], [57.0, -72.33428955078125, -156.0], [57.0, -91.08428955078125, -156.0], [67.0, -116.08428955078125, -166.0], [117.0, -122.33428955078125, -176.0], [57.0, -72.33428955078125, -176.0], [57.0, -84.83428955078125, -176.0], [67.0, -109.83428955078125, -186.0], [117.0, -116.08428955078125, -196.0], [57.0, -78.58428955078125, -206.0], [67.0, -91.08428955078125, -216.0], [87.0, -97.33428955078125, -216.0], [117.0, -103.58428955078125, -216.0], [207.0, -84.83428955078125, -196.0]";
//        var scoreData = "0.0055575598962605, 0.0081764692440629, 0.009658074006438255, 0.00853562168776989, 0.00835314579308033, 0.012276462279260159, 0.009069574996829033, 0.002978245262056589, 0.01668013259768486, 0.012600606307387352, 0.01906680129468441, 0.004842771682888269, 0.01292137335985899, 0.01605168916285038, 0.01967347227036953, 0.004534152802079916, 0.01017997320741415, 0.009401324205100536, 0.009820147417485714, 0.0027163391932845116, 0.0037745358422398567, 0.7456676959991455, 0.7165987491607666, 0.8160498738288879, 0.7610171437263489, 0.6264868378639221, 0.6894381642341614, 0.6975233554840088, 0.7961252331733704, 0.7905790209770203, 0.7951110005378723, 0.8550177216529846, 0.8426766991615295, 0.7840620279312134, 0.8113357424736023, 0.770899772644043, 0.7826843857765198, 0.7189541459083557, 0.7892793416976929, 0.7599977254867554, 0.8353912830352783, 0.9099342823028564";
//          var isRightHand = false;

//
//      var data = "[112.27734375, 37.5, -240.0234375], [577.62890625, -68.75, -693.12890625], [100.03125, -68.75, -240.0234375], [-144.890625, -18.75, -68.578125], [100.03125, -62.5, -252.26953125], [100.03125, -43.75, -240.0234375], [-95.90625, 75.0, -533.9296875], [100.03125, 43.75, -252.26953125], [577.62890625, -131.25, -693.12890625], [100.03125, 68.75, -252.26953125], [577.62890625, -6.25, -693.12890625], [234.73828125, 25.0, -693.12890625], [124.5234375, -125.0, -435.9609375], [-144.890625, -37.5, -484.9453125], [100.03125, 43.75, -252.26953125], [259.23046875, -31.25, -668.63671875], [100.03125, -87.5, -240.0234375], [100.03125, 0.0, -252.26953125], [357.19921875, -87.5, -497.19140625], [112.27734375, -87.5, -240.0234375], [393.9375, 0.0, -472.69921875], [173.5078125, 3.8511157035827637, -460.453125], [234.73828125, -108.64888763427734, -325.74609375], [357.19921875, -83.64888763427734, -435.9609375], [357.19921875, -21.148883819580078, -423.71484375], [283.72265625, -83.64888763427734, -325.74609375], [234.73828125, -52.39888381958008, -325.74609375], [283.72265625, -77.39888763427734, -325.74609375], [283.72265625, 41.35111618041992, -325.74609375], [283.72265625, -96.14888763427734, -325.74609375], [369.4453125, -77.39888763427734, -448.20703125], [283.72265625, -89.89888763427734, -325.74609375], [357.19921875, -64.89888763427734, -435.9609375], [295.96875, -89.89888763427734, -325.74609375], [357.19921875, -108.64888763427734, -435.9609375], [271.4765625, -64.89888763427734, -325.74609375], [357.19921875, -77.39888763427734, -448.20703125], [283.72265625, -21.148883819580078, -325.74609375], [357.19921875, -46.14888381958008, -582.9140625], [271.4765625, -21.148883819580078, -362.484375], [295.96875, 16.351116180419922, -325.74609375], [406.18359375, -2.3988842964172363, -484.9453125]";
//      var isRightHand = false;

    // Kinda works, doesnt appear in the library's visualiser tho
//      var data = "[210.375, 81.25, -214.953125], [282.09375, 56.25, -167.140625], [329.90625, 43.75, -119.328125], [449.4375, 31.25, -191.046875], [90.84375, 75.0, -250.8125], [150.609375, 68.75, -238.859375], [222.328125, 62.5, -250.8125], [317.953125, 43.75, -179.09375], [54.984375, 62.5, -274.71875], [138.65625, 56.25, -250.8125], [210.375, 43.75, -238.859375], [270.140625, 18.75, -286.671875], [54.984375, 50.0, -262.765625], [138.65625, 6.25, -286.671875], [210.375, 6.25, -286.671875], [270.140625, 6.25, -286.671875], [102.796875, 12.5, -286.671875], [150.609375, -12.5, -274.71875], [222.328125, -18.75, -286.671875], [258.1875, -18.75, -298.625], [449.4375, 0.0, -226.90625], [19.125, 29.238784790039062, -274.71875], [114.75, 22.988784790039062, -71.515625], [437.484375, -114.51121520996094, -35.65625], [258.1875, -45.76121520996094, -95.421875], [90.84375, 54.23878479003906, -119.328125], [90.84375, 47.98878479003906, -119.328125], [102.796875, 41.73878479003906, -107.375], [246.234375, -120.76121520996094, -47.609375], [102.796875, 66.73878479003906, -107.375], [102.796875, 47.98878479003906, -71.515625], [138.65625, 41.73878479003906, -83.46875], [102.796875, -45.76121520996094, -95.421875], [54.984375, 29.238784790039062, -214.953125], [54.984375, -14.511215209960938, -131.28125], [90.84375, 16.738784790039062, -95.421875], [150.609375, 4.2387847900390625, -95.421875], [54.984375, 10.488784790039062, -250.8125], [90.84375, 35.48878479003906, -95.421875], [102.796875, 16.738784790039062, -107.375], [210.375, -8.261215209960938, -179.09375], [329.90625, -64.51121520996094, -310.578125]";
//      var isRightHand = true;

    // Hand looking straight forward
//      data = "[112.27734375, 37.5, -240.0234375], [577.62890625, -68.75, -693.12890625], [100.03125, -68.75, -240.0234375], [-144.890625, -18.75, -68.578125], [100.03125, -62.5, -252.26953125], [100.03125, -43.75, -240.0234375], [-95.90625, 75.0, -533.9296875], [100.03125, 43.75, -252.26953125], [577.62890625, -131.25, -693.12890625], [100.03125, 68.75, -252.26953125], [577.62890625, -6.25, -693.12890625], [234.73828125, 25.0, -693.12890625], [124.5234375, -125.0, -435.9609375], [-144.890625, -37.5, -484.9453125], [100.03125, 43.75, -252.26953125], [259.23046875, -31.25, -668.63671875], [100.03125, -87.5, -240.0234375], [100.03125, 0.0, -252.26953125], [357.19921875, -87.5, -497.19140625], [112.27734375, -87.5, -240.0234375], [393.9375, 0.0, -472.69921875], [173.5078125, 3.8511157035827637, -460.453125], [234.73828125, -108.64888763427734, -325.74609375], [357.19921875, -83.64888763427734, -435.9609375], [357.19921875, -21.148883819580078, -423.71484375], [283.72265625, -83.64888763427734, -325.74609375], [234.73828125, -52.39888381958008, -325.74609375], [283.72265625, -77.39888763427734, -325.74609375], [283.72265625, 41.35111618041992, -325.74609375], [283.72265625, -96.14888763427734, -325.74609375], [369.4453125, -77.39888763427734, -448.20703125], [283.72265625, -89.89888763427734, -325.74609375], [357.19921875, -64.89888763427734, -435.9609375], [295.96875, -89.89888763427734, -325.74609375], [357.19921875, -108.64888763427734, -435.9609375], [271.4765625, -64.89888763427734, -325.74609375], [357.19921875, -77.39888763427734, -448.20703125], [283.72265625, -21.148883819580078, -325.74609375], [357.19921875, -46.14888381958008, -582.9140625], [271.4765625, -21.148883819580078, -362.484375], [295.96875, 16.351116180419922, -325.74609375], [406.18359375, -2.3988842964172363, -484.9453125]";
//      var isRightHand = true;

    var data = "[[470.078125, -43.75, -336.328125], [401.71875, -25.0, -377.34375], [374.375, -18.75, -445.703125], [292.34375, -18.75, -527.734375], [333.359375, -12.5, -90.234375], [333.359375, -18.75, -158.59375], [319.6875, -18.75, -226.953125], [306.015625, -12.5, -350.0], [237.65625, -12.5, -62.890625], [251.328125, -12.5, -131.25], [251.328125, -18.75, -213.28125], [251.328125, -12.5, -336.328125], [182.96875, -18.75, -90.234375], [182.96875, -18.75, -158.59375], [182.96875, -18.75, -226.953125], [196.640625, -25.0, -350.0], [114.609375, -37.5, -172.265625], [114.609375, -31.25, -240.625], [128.28125, -37.5, -281.640625], [155.625, -31.25, -363.671875], [237.65625, 0.0, -568.75], [265.0, 77.49695587158203, -267.96875], [100.9375, -28.75304412841797, -199.609375], [114.609375, 46.24695587158203, -172.265625], [46.25, 27.49695587158203, -199.609375], [155.625, 27.49695587158203, -49.21875], [100.9375, 102.49695587158203, -254.296875], [114.609375, 33.74695587158203, -185.9375], [155.625, 14.996955871582031, -254.296875], [223.984375, 39.99695587158203, -35.546875], [155.625, 52.49695587158203, -117.578125], [155.625, 46.24695587158203, -158.59375], [128.28125, 58.74695587158203, -185.9375], [155.625, 52.49695587158203, -90.234375], [155.625, 52.49695587158203, -172.265625], [155.625, 52.49695587158203, -213.28125], [114.609375, 64.99695587158203, -199.609375], [114.609375, 52.49695587158203, -172.265625], [128.28125, 46.24695587158203, -226.953125], [114.609375, 52.49695587158203, -240.625], [141.953125, 58.74695587158203, -350.0], [196.640625, 46.24695587158203, -609.765625]";
    var isRightHand = true;

      var points =
                Arrays.stream(data.split("], "))
                        .map(s -> new Vector(Arrays.stream(
                                 s.replace("[","")
                                  .replace("]","")
                                 .split(", "))
                                        .map(valstr -> Float.parseFloat(valstr))
                                        .toList()
                        ))
                        .toList();
//        var scores = Arrays.stream(scoreData.split(", ")).map(s -> Float.parseFloat(s)).toList();

        var average = new Vector(0,0,0);

        List<Vector> newPoints = new ArrayList<>();
        for(int i = 0; i < 42; i++) {
            if ((isRightHand && i < 21) || (!isRightHand && i >= 21)) {
                newPoints.add(points.get(i));
                average = average.add(points.get(i));
            }
        }

        Vector avg = average.scalarMul(1f/newPoints.size());

        System.out.println("Num of joints = " + newPoints.size());

       newPoints = newPoints.stream().map(p -> p.sub(avg)).toList();

        var vertAttribs = new HashMap<Mesh.VERTATTRIB, List<Vector>>();
        vertAttribs.put(Mesh.VERTATTRIB.POSITION, newPoints);

        var lidarMesh = new Mesh(null, null, vertAttribs, null, "lidar", null);
        lidarMesh.boundingRadius = 100;

        var model = new PointCloud(this, lidarMesh, "Hand", 64);
        model.orientation = Quaternion.getQuaternionFromEuler(-90, 0, 0);
        model.shouldSortVertices = false;
        renderer.addModel(model);

        return model;
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
        var head = new PointCloud(this, meshes, "head", 64);
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
