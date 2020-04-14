package ENED_Simulation;

import engine.DataStructure.Mesh.Mesh;
import engine.DataStructure.Texture;
import engine.Math.Quaternion;
import engine.Math.Vector;
import engine.camera.Camera;
import engine.display.DisplayLWJGL;
import engine.game.Game;
import engine.inputs.InputLWJGL;
import engine.model.Model;
import engine.model.ModelBuilder;

import java.util.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.glViewport;

public class Simulation extends Game {

    protected DisplayLWJGL display;
    protected Camera cam;
    protected InputLWJGL input;
    protected RenderingEngineSim renderingEngine;
    protected List<Model> models;
    protected List<Box> boxes;
    protected List<Box> droppedBoxes;
    Scanner scanner;
    protected int numberOfIPSRequests = 0;

    protected float mouseXSensitivity = 20f;
    protected float mouseYSensitivity = 20f;
    protected float speed = 15f;
    protected float speedMultiplier = 1;
    protected float speedIncreaseMultiplier = 2;

    protected Model lookAtModel;
    protected boolean isGameRunning = true;

    public int simWidth = 100;
    public int simDepth = 100;

    protected List<engine.GUI.Button> pauseButtons;
    protected engine.GUI.Button EXIT;
    protected engine.GUI.Button FULLSCREEN;
    protected engine.GUI.Button WINDOWED;

    protected Vector mouseDelta;
    protected Vector mousePos;

    protected boolean prevGameState = false;

    Map<String, Mesh> meshInstances;
    Robot robot;
    Model flag;

    String robotModelLoc = "/Resources/car.obj";
    String robotTextureLoc = "textures/car.jpg";

    String boxModelLoc = "/Resources/box.obj";
    String boxTextureLoc = "textures/box.png";

    int[] boxRows = {12,24,36,48,60,72,84,96};
    int[] boxCols = {20,35,65,80};
    int boxesPerSide = 1;

    long seed = 123456789;

    public Simulation(String threadName) {
        super(threadName);
    }

    @Override
    public void init() {
        models = new ArrayList<>();
        boxes = new ArrayList<>();
        droppedBoxes = new ArrayList<>();
        meshInstances = new HashMap<>();
        scanner = new Scanner(System.in);

        display = new DisplayLWJGL(this);
        display.startScreen();

        renderingEngine = new RenderingEngineSim(this);
        renderingEngine.init();

        cam = new Camera(this,null,null,null, new Vector(new float[] {0,7,5}),90, 0.001f, 1000,
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
        models = new ArrayList<>();

        initModels();
        initPauseScreen();

        cam.updateValues();
        cam.lookAtModel(flag);

        targetFPS = display.getRefreshRate();

    }

    public void initModels() {
        ModelBuilder.ModelBuilderHints hints = new ModelBuilder.ModelBuilderHints();
        hints.shouldBakeVertexAttributes = false;
        hints.addRandomColor = true;
        hints.initLWJGLAttribs = true;

        hints.addConstantColor = new Vector(new float[]{0.3f,0.3f,0.3f,10});
        Model grid = new Model(ModelBuilder.buildGridLines(simWidth,simDepth,hints),"grid");
        grid.setPos(new Vector(new float[]{0,0,-simDepth}));

        hints.convertToLines = true;
        flag = new Model(ModelBuilder.buildModelFromFileGL("/Resources/objFlag.obj",meshInstances,hints),"flag");
        flag.setPos(new Vector(new float[]{0,10,0}));

        hints.addConstantColor = null;
        hints.convertToLines = true;
        robot = new Robot(this,ModelBuilder.buildModelFromFileGL(robotModelLoc,meshInstances,hints),"robot");
        robot.setPos(new Vector(new float[]{0,1,0}));

        try {
            Texture tex = new Texture(robotTextureLoc);
            robot.mesh.texture = tex;
        } catch (Exception e) {
            e.printStackTrace();
        }

        hints.convertToLines = false;
//        Box box = new Box(ModelBuilder.buildModelFromFileGL(boxModelLoc,meshInstances,hints),"box");
//        try {
//            Texture tex = new Texture(boxTextureLoc);
//            box.mesh.texture = tex;
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
////        boxes.add(box);
//
//        models.addAll(boxes);
        models.add(robot);
        models.add(grid);
        models.add(flag);

        initCrates();

        lookAtModel = robot;

    }

    public void initPauseScreen() {

    }

    public boolean isModelColliding(Model m) {
        return false;
    }

    public void addBoxToDropped(Box box) {
        boxes.remove(box);
        droppedBoxes.add(box);
    }

//    This method initialises boxes
    public void initCrates() {

        Random rand = new Random();
        rand.setSeed(seed);
        Mesh boxMesh;
        Mesh platformMesh;
        float platY = -1;
        float x,y = 1.5f,z;

        ModelBuilder.ModelBuilderHints hints = new ModelBuilder.ModelBuilderHints();
        hints.shouldBakeVertexAttributes = false;
        hints.initLWJGLAttribs = true;

        boxMesh = ModelBuilder.buildModelFromFileGL(boxModelLoc,meshInstances,hints);
        try {
            Texture tex = new Texture(boxTextureLoc);
            boxMesh.texture = tex;
        } catch (Exception e) {
            e.printStackTrace();
        }

        platformMesh = ModelBuilder.buildModelFromFileGL("/Resources/platform2.obj",meshInstances,hints);
        try {
            Texture tex = new Texture("textures/oldwood.jpg");
            platformMesh.texture = tex;
        } catch (Exception e) {
            e.printStackTrace();
        }

        for(int c = 0; c < boxCols.length;c+=2) {
            for (int r = 0; r < boxRows.length; r += 2) {

//            Create shelf model
                Model platform = new Model(platformMesh,"platform-r:"+r+"c:"+c);
                platform.setOrientation(Quaternion.getAxisAsQuat(new Vector(new float[]{0,1,0}),90));

                int tempX = (boxCols[c] + (boxCols[c+1] - boxCols[c])/2);
                int tempZ = -(boxRows[r] + (boxRows[r+1] - boxRows[r])/2) + 1;

                platform.setPos(new Vector(new float[]{tempX,platY,tempZ}));
                platform.setScale(new Vector(new float[]{1f,1f,1f}));
                models.add(platform);

//            Create two rows of boxes
//                create bottom row
                z = -boxRows[r] + 1;
                for(int k = 0; k < boxesPerSide; k++) {
                    x = rand.nextInt(boxCols[c+1] - boxCols[c]) + boxCols[c] - 1;
                    Vector barCode = new Vector(new float[]{rand.nextInt(2),rand.nextInt(2),rand.nextInt(2),rand.nextInt(2)});
                    Box box = new Box(boxMesh,"box-x:"+x+"z:"+z,barCode);
                    box.setPos(new Vector(new float[]{x,y,z}));
                    boxes.add(box);
                }

//                create top row of shelf
                z = -boxRows[r+1] + 1;
                for(int k = 0; k < boxesPerSide; k++) {
                    x = rand.nextInt(boxCols[c+1] - boxCols[c]) + boxCols[c] - 1;
                    Vector barCode = new Vector(new float[]{rand.nextInt(2),rand.nextInt(2),rand.nextInt(2),rand.nextInt(2)});
                    Box box = new Box(boxMesh,"box-x:"+x+"z:"+z,barCode);
                    box.setPos(new Vector(new float[]{x,y,z}));
                    box.setOrientation(Quaternion.getAxisAsQuat(new Vector(new float[]{0,1,0}),180));
                    boxes.add(box);
                }

            }
        }

        models.addAll(boxes);

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
        for(Model m:models) {
            m.mesh.cleanUp();
        }
    }

    @Override
    public void tick() {
        tickInput();

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

        Model.ModelTickInput params = new Model.ModelTickInput();
        params.timeDelta = timeDelta;

        models.forEach(m -> m.tick(params));

        if(!isGameRunning) {
            pauseButtons.forEach((b) -> b.tick(mousePos,((InputLWJGL)input).isLeftMouseButtonPressed));
        }
        else {
            calculate3DCamMovement();
            cam.tick();
        }
    }

    public void tickInput() {

        if(input.keyDown(input.ENTER)) {
            System.out.println("This is the "+(++numberOfIPSRequests)+"th request");
            System.out.println("Enter IPS coordinates: ");
            String text = scanner.nextLine();
            robot.IGPS(text);
        }

        if(input.keyDown(input.TAB)) {
            robot.isManualControl = false;
        }

        if(input.keyDown(GLFW_KEY_W)) {
            float cameraSpeed = speed * timeDelta * speedMultiplier;
            Vector[] rotationMatrix = cam.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector x = rotationMatrix[0];
            Vector y = new Vector(new float[] {0,1,0});
            Vector z = x.cross(y);
            cam.setPos(cam.getPos().sub(z.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(GLFW_KEY_S)) {
            float cameraSpeed = speed * timeDelta * speedMultiplier;
            Vector[] rotationMatrix = cam.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector x = rotationMatrix[0];
            Vector y = new Vector(new float[] {0,1,0});
            Vector z = x.cross(y);
            cam.setPos(cam.getPos().add(z.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(GLFW_KEY_A)) {
            float cameraSpeed = speed * timeDelta * speedMultiplier;
            Vector[] rotationMatrix = cam.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector v = rotationMatrix[0];
            cam.setPos(cam.getPos().sub(v.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(GLFW_KEY_D)) {
            float cameraSpeed = speed * timeDelta * speedMultiplier;
            Vector[] rotationMatrix = cam.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector v = rotationMatrix[0];
            cam.setPos(cam.getPos().add(v.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(GLFW_KEY_SPACE)) {
            float cameraSpeed = speed * timeDelta * speedMultiplier;

            Vector v = new Vector(new float[] {0,1,0});
            cam.setPos(cam.getPos().add(v.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(GLFW_KEY_LEFT_SHIFT)) {
            float cameraSpeed = speed * timeDelta * speedMultiplier;

            Vector v = new Vector(new float[] {0,1,0});
            cam.setPos(cam.getPos().sub(v.scalarMul(cameraSpeed)));
        }

        if(input.keyDownOnce(GLFW_KEY_ESCAPE)) {
            isGameRunning = !isGameRunning;
        }

        if(isGameRunning) {
            if(input.keyDownOnce(GLFW_KEY_R)) {
                cam.lookAtModel(lookAtModel);
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

            Vector currentAngle = cam.getOrientation().getPitchYawRoll();
            float currentPitch = currentAngle.get(0) + pitchIncrease;

            if(currentPitch >= 0 && currentPitch > 60) {
                pitchIncrease = 0;
            }
            else if(currentPitch < 0 && currentPitch < -60) {
                pitchIncrease = 0;
            }

            Quaternion pitch = Quaternion.getAxisAsQuat(new Vector(new float[] {1,0,0}),pitchIncrease);
            Quaternion yaw = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}),yawIncrease);

            Quaternion q = cam.getOrientation();

            q = q.multiply(pitch);
            q = yaw.multiply(q);
            cam.setOrientation(q);
        }

    }

    @Override
    public void render() {
        renderingEngine.render(models);
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
        return cam;
    }

    @Override
    public InputLWJGL getInput() {
        return input;
    }

    @Override
    public List<Model> getModels() {
        return models;
    }
}
