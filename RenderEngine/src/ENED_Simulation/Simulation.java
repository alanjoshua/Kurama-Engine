package ENED_Simulation;

import engine.model.ModelBehaviourTickInput;
import engine.renderingEngine.defaultRenderPipeline.DefaultRenderPipeline;
import engine.Mesh.Mesh;
import engine.DataStructure.Texture;
import engine.Effects.Material;
import engine.Effects.ShadowMap;
import engine.Math.Matrix;
import engine.Math.Quaternion;
import engine.Math.Vector;
import engine.camera.Camera;
import engine.display.Display;
import engine.display.DisplayLWJGL;
import engine.game.Game;
import engine.inputs.InputLWJGL;
import engine.lighting.DirectionalLight;
import engine.lighting.PointLight;
import engine.Mesh.MeshBuilder;
import engine.Mesh.MeshBuilderHints;
import engine.model.Model;
import engine.scene.Scene;

import java.util.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.glViewport;

public class Simulation extends Game {

    protected DisplayLWJGL display;
//    protected Camera cam;
    protected InputLWJGL input;
    protected RenderingEngineSim renderingEngine;
    protected List<Box> boxesToBeSearched;
    protected List<Box> boxesAlreadySearched;
    protected List<Box> boxesAtDestination;
    Scanner scanner;
    protected int numberOfIPSRequests = 0;

    protected float mouseXSensitivity = 20f;
    protected float mouseYSensitivity = 20f;
    protected float speed = 15f;
    protected float speedMultiplier = 1;
    protected float speedIncreaseMultiplier = 2;

    protected Model lookAtModel;
    protected boolean isGameRunning = true;
    public boolean shouldOnlyOutline = false;
    public boolean barcodeRequestShouldAskUser = false;

    public int simWidth = 100;
    public int simDepth = 100;

    protected List<engine.GUI.Button> pauseButtons;
    protected engine.GUI.Button EXIT;
    protected engine.GUI.Button FULLSCREEN;
    protected engine.GUI.Button WINDOWED;

    protected Vector mouseDelta;
    protected Vector mousePos;

    protected boolean prevGameState = false;

    Robot robot;
    Model flag;

    String robotModelLoc = "/Resources/car.obj";
    String robotTextureLoc = "textures/car.jpg";

    String boxModelLoc = "/Resources/box.obj";
    String boxTextureLoc = "textures/box.png";

    int[] boxRows = {24,36,48,60,72,84,96,108};
    int[] boxCols = {12,48,60,96};
    int boxesPerSide = 1;

    long seed = 123456789;
    Vector towerA,towerB,towerC,towerD;

    public Material pathMat;
    public Material boxWrongMat;
    public  Material boxRequiredMat;
    public Material nullMat;

    public Simulation(String threadName) {
        super(threadName);
    }

    @Override
    public void init() {

        scene = new Scene(this);

        Vector lightColor = new Vector(new float[]{1f,1f,1f});
        Vector lightPos = new Vector(new float[]{0f,0f,1f});
        float lightIntensity = 1f;
        PointLight pointLight = new PointLight(lightColor,lightPos,lightIntensity);
        pointLight.attenuation = new PointLight.Attenuation(0f,0f,1f);
        scene.pointLights.add(pointLight);

        scene.directionalLights.add(new DirectionalLight(this,new Vector(new float[]{1,1,1}),
                Quaternion.getQuaternionFromEuler(90,0,0),1,new ShadowMap(ShadowMap.DEFAULT_SHADOWMAP_WIDTH * 1,
                ShadowMap.DEFAULT_SHADOWMAP_HEIGHT * 1), (Mesh) null, null, null, "light"));  //Add shadowlight projection code
        scene.directionalLights.get(0).shadowProjectionMatrix = Matrix.buildOrthographicProjectionMatrix(1,-700,100,-100,-100,100);
        boxesToBeSearched = new ArrayList<>();
        boxesAlreadySearched = new ArrayList<>();
        boxesAtDestination = new ArrayList<>();
        scanner = new Scanner(System.in);

        towerA = new Vector(new float[]{6,1,-6});
        towerB = new Vector(new float[]{102,1,-6});
        towerC = new Vector(new float[]{6,1,-126});
        towerD = new Vector(new float[]{102,1,-126});

        simDepth = 132;
        simWidth = 108;

        boundMin = new Vector(new float[]{0,0,-simDepth});
        boundMax = new Vector(new float[]{simWidth,100,0});

        display = new DisplayLWJGL(this);
        display.startScreen();

        renderingEngine = new RenderingEngineSim(this);
        renderingEngine.init(scene);

        scene.camera = new Camera(this,null, new Vector(new float[] {0,7,5}),90, 0.001f, 1000,
                display.getWidth(), display.getHeight());

        glfwSetFramebufferSizeCallback(display.getWindow(), (window, width, height) -> {
            glViewport(0,0,width,height);
            if(getCamera() != null) {
                display.setWIDTH(width);
                display.setHEIGHT(height);
                getCamera().setImageWidth(width);
                getCamera().setImageHeight(height);
                getCamera().setShouldUpdateValues(true);
            }
        });

        input = new InputLWJGL(this);

        pauseButtons = new ArrayList<>();

        boxWrongMat = new Material(new Vector(new float[]{1,0,0,1}),1, "boxWrong");
        boxRequiredMat = new Material(new Vector(new float[]{0,1,0,1}),1, "boxRequired");
        pathMat = new Material(new Vector(new float[]{0,1,0,1}),1, "pathMat");
        nullMat = new Material(new Vector(new float[]{0,0,0,0}),0, "nullMat");

        initModels();
        initPauseScreen();

        scene.camera.updateValues();
        scene.camera.lookAtModel(flag);

        targetFPS = display.getRefreshRate();
        scene.hud = new SimulationHUD(this);
    }

    public void initModels() {

        DefaultRenderPipeline renderPipeline = (DefaultRenderPipeline)scene.renderPipeline;

        MeshBuilderHints hints = new MeshBuilderHints();
        hints.shouldSmartBakeVertexAttributes = false;
        hints.addRandomColor = true;
        hints.initLWJGLAttribs = true;

        hints.addConstantColor = new Vector(new float[]{1f,1,1f,1f});
        Model grid = new Model(this, MeshBuilder.buildGridLines(simWidth,simDepth,hints),"grid");
        grid.setPos(new Vector(new float[]{0,0,-simDepth}));
        grid.isCollidable = false;
        grid.meshes.get(0).materials.get(0).ambientColor = new Vector(new float[]{1,1,1,1});

        hints.convertToLines = true;
        flag = new Model(this, MeshBuilder.buildMesh("/Resources/objFlag.obj",hints),"flag");
        flag.setPos(new Vector(new float[]{0,10,0}));
        flag.isCollidable = false;

        hints.addConstantColor = null;
        hints.addRandomColor = true;
        Model h1 = new Model(this, MeshBuilder.buildGridTrigs(12,12,hints),"h1");
        h1.setPos(0,0.1f,-12);
        h1.isCollidable = false;
//        h1.shouldUseMaterial = false;

        Model h2 = new Model(this, MeshBuilder.buildGridTrigs(12,12,hints),"h2");
        h2.setPos(96,0.1f,-12);
        h2.isCollidable = false;

        Model h3 = new Model(this, MeshBuilder.buildGridTrigs(12,12,hints),"h3");
        h3.setPos(0,0.1f,-132);
        h3.isCollidable = false;

        Model h4 = new Model(this, MeshBuilder.buildGridTrigs(12,12,hints),"h4");
        h4.setPos(96,0.1f,-132);
        h4.isCollidable = false;

        hints.addConstantColor = null;
        hints.convertToLines = true;
        robot = new Robot(this, MeshBuilder.buildMesh(robotModelLoc,hints),"robot");
        robot.shouldShowCollisionBox = false;
        robot.shouldShowPath = true;
        robot.home = towerA;
        robot.setPos(robot.home);

        try {
            Texture tex = new Texture(robotTextureLoc);
            robot.meshes.get(0).materials.get(0).texture = tex;
        } catch (Exception e) {
            e.printStackTrace();
        }

        float skyBoxScale = 500;

        hints.shouldSmartBakeVertexAttributes = true;
        hints.convertToLines = false;
        scene.skybox = new Model(this, MeshBuilder.buildMesh("/Resources/skybox.obj",hints),"skybox");
        scene.skybox.setScale(skyBoxScale);
        Texture tex = null;
        try {
            tex = new Texture("textures/skybox.png");
        }catch (Exception e) {
            System.out.println("Couldn't load skybox texture");
        }
        Material skyMat = new Material(tex,1, "skyMat");
        skyMat.ambientColor = new Vector(new float[]{1,1,1,1});
        scene.skybox.meshes.get(0).materials.set(0,skyMat);

        hints.convertToLines = false;

        scene.addModel(robot, Arrays.asList(new String[]{renderPipeline.sceneShaderBlockID}));
        scene.addModel(grid, Arrays.asList(new String[]{renderPipeline.sceneShaderBlockID}));
        scene.addModel(flag, Arrays.asList(new String[]{renderPipeline.sceneShaderBlockID}));
        scene.addModel(h1, Arrays.asList(new String[]{renderPipeline.sceneShaderBlockID}));
        scene.addModel(h2, Arrays.asList(new String[]{renderPipeline.sceneShaderBlockID}));
        scene.addModel(h3, Arrays.asList(new String[]{renderPipeline.sceneShaderBlockID}));
        scene.addModel(h4, Arrays.asList(new String[]{renderPipeline.sceneShaderBlockID}));

        initCrates();

        lookAtModel = robot;

    }

    public void initPauseScreen() {

    }

    public void addBoxToSearched(Box box) {
        boxesToBeSearched.remove(box);
        boxesAlreadySearched.add(box);
    }

    public void addBoxToAtDestination(Box box) {
        box.isCollidable = false;
        boxesToBeSearched.remove(box);
        boxesAtDestination.add(box);
    }

//    This method initialises boxes
    public void initCrates() {
        DefaultRenderPipeline renderPipeline = (DefaultRenderPipeline)scene.renderPipeline;

        Random rand = new Random();
        //rand.setSeed(seed);
        Mesh boxMesh;
        Mesh platformMesh;
        float platY = 1;
        float x,y = 1.5f,z;

        List<Vector> barcodes = new ArrayList<>();

//        Model.MiniBehaviour tempRot = ((m, params) -> {
//            Quaternion rot = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}), 50* timeDelta);
//            Quaternion newQ = rot.multiply(m.getOrientation());
//            m.setOrientation(newQ);
//        });

        MeshBuilderHints hints = new MeshBuilderHints();
        hints.shouldSmartBakeVertexAttributes = false;
        hints.initLWJGLAttribs = true;

        boxMesh = MeshBuilder.buildMesh(boxModelLoc,hints);
        try {
            Texture tex = new Texture(boxTextureLoc);
            boxMesh.materials.get(0).texture = tex;
        } catch (Exception e) {
            e.printStackTrace();
        }

        platformMesh = MeshBuilder.buildMesh("/Resources/platform2.obj",hints);
        try {
            Texture tex = new Texture("textures/oldwood.jpg");
            platformMesh.materials.get(0).texture = tex;
        } catch (Exception e) {
            e.printStackTrace();
        }

        Vector[] bounds = Model.getBounds(platformMesh);
        float platScaleY = 0.5f;

        float requiredZ = boxRows[1] - boxRows[0] - 2;
        float currentZ = bounds[1].get(2) - bounds[0].get(2);
        float platScaleZ = requiredZ / currentZ;

        float requiredX = boxCols[1] - boxCols[0] - 1;
        float currentX = bounds[1].get(0) - bounds[0].get(0);
        float platScaleX = requiredX / currentX;

        bounds = Model.getBounds(boxMesh);
        float requiredZBox = 4;
        float currentZBox = bounds[1].get(2) - bounds[0].get(2);
        float boxScaleZ = requiredZBox / currentZBox;

        float requiredXBox = 4;
        float currentXBox = bounds[1].get(0) - bounds[0].get(0);
        float boxScaleX = requiredXBox / currentXBox;

        float requiredYBox = 6;
        float currentYBox = bounds[1].get(1) - bounds[0].get(1);
        float boxScaleY = requiredYBox / currentYBox;

        for(int c = 0; c < boxCols.length;c+=2) {
            for (int r = 0; r < boxRows.length; r += 2) {

//            Create shelf model
                Model platform = new Model(this,platformMesh,"platform-r:"+r+"c:"+c);
                //platform.setOrientation(Quaternion.getAxisAsQuat(new Vector(new float[]{0,1,0}),90));

                int tempX = (boxCols[c] + (boxCols[c+1] - boxCols[c])/2);
                int tempZ = -(boxRows[r] + (boxRows[r+1] - boxRows[r])/2)+1;

                platform.setPos(new Vector(new float[]{tempX,platY,tempZ}));
                platform.setScale(new Vector(new float[]{platScaleX,platScaleY,platScaleZ}));
                scene.addModel(platform, Arrays.asList(new String[]{renderPipeline.sceneShaderBlockID}));

                Integer currZone = getZone(r,c);

//            Create two rows of boxes
//                create bottom row
                z = -boxRows[r] + 1;
                for(int k = 0; k < boxesPerSide; k++) {
                    x = rand.nextInt(boxCols[c+1] - boxCols[c]) + boxCols[c] - 1;

                    Vector barCode = new Vector(new float[]{rand.nextInt(2),rand.nextInt(2),rand.nextInt(2),rand.nextInt(2)});
                    while(barcodes.indexOf(barCode) != -1) {
                        barCode = new Vector(new float[]{rand.nextInt(2),rand.nextInt(2),rand.nextInt(2),rand.nextInt(2)});
                    }
                    barcodes.add(barCode);

                    Box box = new Box(this,boxMesh,"box:: x:"+x+" z:"+z,barCode,currZone);
                    box.setPos(new Vector(new float[]{x,y,z}));
                    //box.setScale(boxScaleX,boxScaleY,boxScaleZ);
                    boxesToBeSearched.add(box);
                }

//                create top row of shelf
                z = -boxRows[r+1] + 1;
                for(int k = 0; k < boxesPerSide; k++) {
                    x = rand.nextInt(boxCols[c+1] - boxCols[c]) + boxCols[c] - 1;

                    Vector barCode = new Vector(new float[]{rand.nextInt(2),rand.nextInt(2),rand.nextInt(2),rand.nextInt(2)});
                    while(barcodes.indexOf(barCode) != -1) {
                        barCode = new Vector(new float[]{rand.nextInt(2),rand.nextInt(2),rand.nextInt(2),rand.nextInt(2)});
                    }
                    barcodes.add(barCode);

                    Box box = new Box(this,boxMesh,"box:: x:"+x+" z:"+z,barCode,currZone);
                    box.setPos(new Vector(new float[]{x,y,z}));
                    //box.setScale(boxScaleX,boxScaleY,boxScaleZ);
                    box.setOrientation(Quaternion.getAxisAsQuat(new Vector(new float[]{0,1,0}),180));
                    boxesToBeSearched.add(box);
                }

            }
        }

        for (Box b: boxesToBeSearched) {
            scene.addModel(b, Arrays.asList(new String[]{renderPipeline.sceneShaderBlockID}));
        }
//        scene.getModels().addAll(boxesToBeSearched);

    }

    public Integer getZone(int r, int c) {

        if(r <= 3 && c <= 1) {
            return Box.ZONE_A;
        }

        if(r <= 3 && c > 1) {
            return Box.ZONE_B;
        }

        if(r > 3 && c <= 1) {
            return Box.ZONE_C;
        }

        if(r > 3 && c > 1) {
            return Box.ZONE_D;
        }

        return null;
    }

    public Box requestNextBarcode() {
        boxesToBeSearched.addAll(boxesAlreadySearched);
        boxesAlreadySearched = new ArrayList<>();
        for(Box b: boxesToBeSearched) {
            b.shouldShowCollisionBox = false;
        }

        if(barcodeRequestShouldAskUser) {
            isGameRunning = false;
            Display.DisplayMode mode = display.displayMode;
            if(mode == Display.DisplayMode.FULLSCREEN) {
                display.setWindowedMode();
            }

            System.out.println("These are the barcodes of the boxes still present on shelves: ");
            boxesToBeSearched.forEach(b -> b.barCode.display());
            System.out.println("--------------------------");
            System.out.println("Please enter a barcode (leave whitespace between individual digits)");

            String input = scanner.nextLine();
            String[] split = input.split("\\s+");
            float[] data = new float[split.length];

            try {
                for(int i = 0;i < split.length;i++) {
                    data[i] = Float.parseFloat(split[i]);
                }
                Vector barcode = new Vector(data);
               Optional<Box> optional = boxesToBeSearched.stream()
                                .filter(b -> b.barCode.equals(barcode))
                                .findFirst();

               if(optional.isPresent()) {
                   Box ret = optional.get();
                   ret.setBoundingBoxColor(new Vector(new float[]{0, 1, 0, 1}));
                   ret.getBoundingBox().materials.set(0,boxRequiredMat);
                   ret.shouldShowCollisionBox = true;

                   if (mode == Display.DisplayMode.FULLSCREEN) {
                       display.setFullScreen();
                   }
                   return ret;
               }
                else {
                   Box box = new Box(this, null, "temp", barcode,null);

                   if (mode == Display.DisplayMode.FULLSCREEN) {
                       display.setFullScreen();
                   }
                   return box;
               }

            }catch (Exception e) {
                System.out.println("Failed to convert user input as barcode. Barcode being randomly selected from available boxes. Press 4 to request new barcode next time");
                barcodeRequestShouldAskUser = false;
            }

            if(mode == Display.DisplayMode.FULLSCREEN) {
                display.setWindowedMode();
            }
            return null;
        }
        else {
            Random rand = new Random();
            if (boxesToBeSearched.size() == 0) {
                return null;
            }
            Box ret = boxesToBeSearched.get(rand.nextInt(boxesToBeSearched.size()));
            ret.setBoundingBoxColor(new Vector(new float[]{0, 1, 0, 1}));
            ret.shouldShowCollisionBox = true;
            ret.getBoundingBox().materials.set(0,boxRequiredMat);
            return ret;
        }

    }

    @Override
    public void start() {
        String osName = System.getProperty("os.name");
        if ( osName.contains("Mac") ) {
            gameLoopThread.run();   //To make this program compatible with macs
        } else {
            System.out.println("start called");
            gameLoopThread.start();
        }
    }

    @Override
    public void cleanUp() {
        display.cleanUp();
        renderingEngine.cleanUp();
        scene.cleanUp();
    }

    @Override
    public void tick() {
        tickInput();
        scene.hud.tick();

        if(glfwWindowShouldClose(((DisplayLWJGL)display).getWindow())) {
            programRunning = false;
        }

        mouseDelta = input.getDelta();
        mousePos = input.getPos();

        if(isGameRunning != prevGameState) {
            if (isGameRunning)
                display.disableCursor();
            else
                display.enableCursor();
            prevGameState = isGameRunning;
        }

        if(isGameRunning) {
            ModelBehaviourTickInput params = new ModelBehaviourTickInput(timeDelta, scene);

            scene.getModels()
                    .stream()
                    .filter(m -> m != robot)
                    .forEach(m -> m.tick(params));

            robot.tick(params);
        }

        if(!isGameRunning) {
            pauseButtons.forEach((b) -> b.tick(mousePos,((InputLWJGL)input).isLeftMouseButtonPressed));
        }
        else {
            calculate3DCamMovement();
            scene.camera.tick();
        }
    }

    public int[][] createCollisionArray(List<Model> modelsToAvoidChecking) {
        int[][] collisionArray = new int[simWidth][simDepth];
        List<Vector> boundData = new ArrayList<>();

        for(Model m: scene.getModels()) {
            if(m.isCollidable && modelsToAvoidChecking.indexOf(m) == -1) {  //Model is collidable and also not in avoid list
                if(shouldOnlyOutline) {
                   boundData.addAll(getModelOutlineCollisionData(m));
                }
                else {
                    boundData.addAll(getModelFillCollisionData(m));
                }

            }
        }

        for(Vector v:boundData) {
            int i = (int)v.get(0);
            int j = -(int)v.get(2);
            collisionArray[i][j] = 1;
        }

        return collisionArray;

    }

    public List<Vector> getModelFillCollisionData(Model m) {
        List<Vector> boundData = new ArrayList<>();

        List<Vector> vertices = new ArrayList<>();
        vertices.add(m.getBoundingBox().getVertices().get(0));
        vertices.add(m.getBoundingBox().getVertices().get(2));
        vertices.add(m.getBoundingBox().getVertices().get(4));
        vertices.add(m.getBoundingBox().getVertices().get(6));

        vertices = m.getObjectToWorldMatrix().matMul(vertices).convertToColumnVectorList();

        Vector v1 = vertices.get(0);
        Vector v2 = vertices.get(1);
        Vector v3 = vertices.get(2);
        Vector v4 = vertices.get(3);

        Vector edge1 = v2.sub(v1);
        Vector edge2 = v4.sub(v1);

        int dist1 = (int)edge1.getNorm();
        int dist2 = (int)edge2.getNorm();

        edge1 = edge1.normalise();
        edge2 = edge2.normalise();

        for (int t1 = 0; t1 <= dist1; t1++) {
            Vector p1 = edge1.scalarMul(t1).add(v1);

            for (int t2 = 0; t2 <= dist2; t2++) {
                Vector p2 = p1.add(edge2.scalarMul(t2));
                int i = (int) p2.get(0);
                int j = -(int) p2.get(2);
                if (i >= 0 && i < simWidth && j >= 0 && j < simDepth) {
                    boundData.add(p2);
                }
            }
        }

        return boundData;
    }

    public List<Vector> getModelOutlineCollisionData(Model m) {
        List<Vector> boundData = new ArrayList<>();

        List<Vector> vertices = new ArrayList<>();
        vertices.add(m.getBoundingBox().getVertices().get(0));
        vertices.add(m.getBoundingBox().getVertices().get(2));
        vertices.add(m.getBoundingBox().getVertices().get(4));
        vertices.add(m.getBoundingBox().getVertices().get(6));

        vertices = m.getObjectToWorldMatrix().matMul(vertices).convertToColumnVectorList();

        Vector v1 = vertices.get(0);
        Vector v2 = vertices.get(1);
        Vector v3 = vertices.get(2);
        Vector v4 = vertices.get(3);

        Vector edge1 = v2.sub(v1);
        Vector edge2 = v4.sub(v1);

        int dist1 = (int)edge1.getNorm();
        int dist2 = (int)edge2.getNorm();

        edge1 = edge1.normalise();
        edge2 = edge2.normalise();

        for (int t1 = 0; t1 <= dist1; t1++) {
            Vector p1 = edge1.scalarMul(t1).add(v1);
            int i = (int) p1.get(0);
            int j = -(int) p1.get(2);
            if (i >= 0 && i < simWidth && j >= 0 && j < simDepth) {
                boundData.add(p1);
            }
        }

        for (int t1 = 0; t1 <= dist1; t1++) {
            Vector p1 = edge1.scalarMul(t1).add(v4);
            int i = (int) p1.get(0);
            int j = -(int) p1.get(2);
            if (i >= 0 && i < simWidth && j >= 0 && j < simDepth) {
                boundData.add(p1);
            }
        }

        for (int t1 = 0; t1 <= dist2; t1++) {
            Vector p1 = edge2.scalarMul(t1).add(v1);
            int i = (int) p1.get(0);
            int j = -(int) p1.get(2);
            if (i >= 0 && i < simWidth && j >= 0 && j < simDepth) {
                boundData.add(p1);
            }
        }

        for (int t1 = 0; t1 <= dist2; t1++) {
            Vector p1 = edge2.scalarMul(t1).add(v2);
            int i = (int) p1.get(0);
            int j = -(int) p1.get(2);
            if (i >= 0 && i < simWidth && j >= 0 && j < simDepth) {
                boundData.add(p1);
            }
        }

        return boundData;

    }

    public void tickInput() {

        if(input.keyDown(input.ENTER)) {

            isGameRunning = false;
            Display.DisplayMode mode = display.displayMode;
            if(mode == Display.DisplayMode.FULLSCREEN) {
                display.setWindowedMode();
            }

            System.out.println("This is the "+(++numberOfIPSRequests)+"th request");
            System.out.println("Enter IPS coordinates: ");
            String text = scanner.nextLine();
            robot.IGPS(text);
            isGameRunning = true;

            if(mode == Display.DisplayMode.FULLSCREEN) {
                display.setFullScreen();
            }
        }

        if(input.keyDown(GLFW_KEY_W)) {
            float cameraSpeed = speed * timeDelta * speedMultiplier;
            Vector[] rotationMatrix = scene.camera.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector x = rotationMatrix[0];
            Vector y = new Vector(new float[] {0,1,0});
            Vector z = x.cross(y);
            scene.camera.setPos(scene.camera.getPos().sub(z.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(GLFW_KEY_S)) {
            float cameraSpeed = speed * timeDelta * speedMultiplier;
            Vector[] rotationMatrix = scene.camera.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector x = rotationMatrix[0];
            Vector y = new Vector(new float[] {0,1,0});
            Vector z = x.cross(y);
            scene.camera.setPos(scene.camera.getPos().add(z.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(GLFW_KEY_A)) {
            float cameraSpeed = speed * timeDelta * speedMultiplier;
            Vector[] rotationMatrix = scene.camera.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector v = rotationMatrix[0];
            scene.camera.setPos(scene.camera.getPos().sub(v.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(GLFW_KEY_D)) {
            float cameraSpeed = speed * timeDelta * speedMultiplier;
            Vector[] rotationMatrix = scene.camera.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector v = rotationMatrix[0];
            scene.camera.setPos(scene.camera.getPos().add(v.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(GLFW_KEY_SPACE)) {
            float cameraSpeed = speed * timeDelta * speedMultiplier;

            Vector v = new Vector(new float[] {0,1,0});
            scene.camera.setPos(scene.camera.getPos().add(v.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(GLFW_KEY_LEFT_SHIFT)) {
            float cameraSpeed = speed * timeDelta * speedMultiplier;

            Vector v = new Vector(new float[] {0,1,0});
            scene.camera.setPos(scene.camera.getPos().sub(v.scalarMul(cameraSpeed)));
        }

        if(input.keyDownOnce(GLFW_KEY_ESCAPE)) {
            isGameRunning = !isGameRunning;
        }

        if(input.keyDownOnce(input.FOUR)) {
            barcodeRequestShouldAskUser = true;
        }

        if(isGameRunning) {
            if(input.keyDownOnce(GLFW_KEY_R)) {
                scene.camera.lookAtModel(lookAtModel);
            }

            if(input.keyDownOnce(GLFW_KEY_LEFT_CONTROL)) {
                if(speedMultiplier == 1) speedMultiplier = speedIncreaseMultiplier;
                else speedMultiplier = 1;
            }

            if(input.keyDownOnce(GLFW_KEY_F)) {
                if(targetFPS == ((DisplayLWJGL)display).getRefreshRate()) {
                    targetFPS = 10000;
                }
                else {
                    targetFPS = ((DisplayLWJGL)display).getRefreshRate();
                }
            }

//            if(input.keyDownOnce(GLFW_KEY_Q)) {
//                if(renderingEngine.getRenderPipeline() == RenderingEngineLWJGL.RenderPipeline.Quat) renderingEngine.setRenderPipeline(RenderingEngineLWJGL.RenderPipeline.Matrix);
//                else renderingEngine.setRenderPipeline(RenderingEngineLWJGL.RenderPipeline.Quat);
//            }

            if(input.keyDownOnce(GLFW_KEY_V)) {
                display.toggleWindowModes();
            }
        }
    }

    public void calculate3DCamMovement() {
        if (mouseDelta.getNorm() != 0 && isGameRunning) {

            float yawIncrease   = mouseXSensitivity * timeDelta * -mouseDelta.get(0);
            float pitchIncrease = mouseYSensitivity * timeDelta * -mouseDelta.get(1);

            Vector currentAngle = scene.camera.getOrientation().getPitchYawRoll();
            float currentPitch = currentAngle.get(0) + pitchIncrease;

            if(currentPitch >= 0 && currentPitch > 60) {
                pitchIncrease = 0;
            }
            else if(currentPitch < 0 && currentPitch < -60) {
                pitchIncrease = 0;
            }

            Quaternion pitch = Quaternion.getAxisAsQuat(new Vector(new float[] {1,0,0}),pitchIncrease);
            Quaternion yaw = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}),yawIncrease);

            Quaternion q = scene.camera.getOrientation();

            q = q.multiply(pitch);
            q = yaw.multiply(q);
            scene.camera.setOrientation(q);
        }

    }

    @Override
    public void render() {
        boolean hasAddedPath = false;
        if(robot.pathModel!= null && robot.shouldShowPath) {
//           scene.addModel(robot.pathModel);
//           List<Model> tempList = new ArrayList<>();
//           tempList.add(robot.pathModel);
//           scene.mesh_model_map.put(robot.pathModel.mesh,tempList);
//           scene.addModel(robot.pathModel);
            robot.pathModel.shouldRender = true;
           hasAddedPath = true;
        }
        renderingEngine.render(scene);
        if(hasAddedPath) {
//            scene.mesh_model_map.remove(robot.pathModel.mesh);
//            scene.models.remove(scene.models.size()-1);
            robot.pathModel.shouldRender = false;
        }

        glfwSwapBuffers(display.getWindow());
        glfwPollEvents();
        input.poll();
    }

    @Override
    public RenderingEngineSim getRenderingEngine() {
        return renderingEngine;
    }

    @Override
    public DisplayLWJGL getDisplay() {
        return display;
    }

    @Override
    public Camera getCamera() {
        return scene.camera;
    }

    @Override
    public InputLWJGL getInput() {
        return input;
    }
}
