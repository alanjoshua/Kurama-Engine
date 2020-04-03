package main;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import GUI.ButtonLWJGL;
import Math.Quaternion;
import Math.Vector;
import inputs.InputLWJGL;
import models.DataStructure.Mesh.Mesh;
import models.Model;
import models.Model.Tick;
import models.ModelBuilder;
import rendering.CameraLWJGL;
import rendering.RenderingEngineLWJGL.RenderPipeline;
import rendering.RenderingEngineLWJGL.ProjectionMode;
import rendering.RenderingEngineLWJGL;

import org.lwjgl.glfw.*;

import static org.lwjgl.glfw.GLFW.*;

public class GameLWJGL implements Runnable {

    protected DisplayLWJGL display;
    protected List<Model> models;
    protected List<GUI.ButtonLWJGL> pauseButtons;
    protected double targetFPS = 100000;
    protected boolean shouldDisplayFPS = false;
    protected boolean programRunning = true;
    protected CameraLWJGL cam;
    protected InputLWJGL input;
    protected boolean isGameRunning = true;
    protected boolean prevGameState = false;
    protected float fps;
    protected float displayFPS;
    protected float mouseXSensitivity = 20f;
    protected float mouseYSensitivity = 20f;
    protected float speed = 15f;
    protected float speedMultiplier = 1;
    protected float speedIncreaseMultiplier = 2;
    protected float speedConstant;
    protected int lookAtIndex = 0;

    protected ButtonLWJGL EXIT;
    protected ButtonLWJGL FULLSCREEN;
    protected ButtonLWJGL WINDOWED;

    protected RenderingEngineLWJGL renderingEngine;
    protected List<Model> modelsOldRenderMethod;

    protected Vector mouseDelta;
    protected Vector mousePos;

    private Thread gameLoopThread;

    Map<String, Mesh> meshInstances;

    public GameLWJGL(int width, int height) {
        gameLoopThread = new Thread(this,"Game Thread");
    }

    public GameLWJGL() {
        gameLoopThread = new Thread(this,"Game Thread");
    }

    public void start() {
        String osName = System.getProperty("os.name");
        if ( osName.contains("Mac") ) {
            gameLoopThread.run();   //To make this program compatible with macs
        } else {
            gameLoopThread.start();
        }
    }

    public void run() {
        try {
            init();
        } catch (Exception e) {
            System.err.println("Could not init");
            e.printStackTrace();
        }
        runGame();
    }

    public void init() throws Exception {
        meshInstances = new HashMap<>();
        display = new DisplayLWJGL(this);
        renderingEngine = new RenderingEngineLWJGL(this);

        display.startScreen();
        renderingEngine.init();

        input = new InputLWJGL(display.getWindow());
        pauseButtons = new ArrayList<>();
        models = new ArrayList<>();
        modelsOldRenderMethod = new ArrayList<>();

        cam = new CameraLWJGL(this,null,null,null, new Vector(new float[] {0,7,5}),90, 0.001f, 1000,
                display.getWidth(), display.getHeight());

        initModels();
        initPauseScreen();

        initInputControls();

        renderingEngine.setProjectionMode(ProjectionMode.PERSPECTIVE);
        renderingEngine.setRenderPipeline(RenderPipeline.Matrix);

        cam.updateValues();
        cam.lookAtModel(models.get(lookAtIndex));

//        cam.setPos(models.get(lookAtIndex).getCentre());

    }

    public void initModels() {
        Tick tempRot = (m -> {
            Quaternion rot = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}), 50*speedConstant);
            Quaternion newQ = rot.multiply(m.getOrientation());
            m.setOrientation(newQ);
        });

        Model deer = ModelBuilder.buildModelFromFile("deer.obj",meshInstances);
        deer.setPos(new Vector(new float[] {-10,18,-15}));
        deer.setScale(new Vector(new float[] { 0.01f, 0.01f, 0.01f }));
        deer.mesh.initOpenGLMeshData();
        deer.setTickObj(tempRot);

        Model deer2 = ModelBuilder.buildModelFromFile("deer.obj",meshInstances);
        deer2.setPos(new Vector(new float[] {0,18,0}));
        deer2.setScale(new Vector(new float[] { 0.01f, 0.01f, 0.01f }));
        deer2.mesh.initOpenGLMeshData();
        deer2.setTickObj(tempRot);

        Model mill = ModelBuilder.buildModelFromFile("low-poly-mill.obj",meshInstances);
        mill.setPos(new Vector(new float[] {10,5,0}));
        mill.setScale(new Vector(new float[] { 0.5f, 0.5f, 0.5f }));
        mill.mesh.initOpenGLMeshData();

        Model grid = ModelBuilder.buildGrid(100, 100);
        grid.setPos(new Vector(new float[] {0,0,0}));

        Model pot = ModelBuilder.buildModelFromFile("TeapotHex3.obj",meshInstances);
        pot.setPos(new Vector(new float[]{0,10,10}));
        pot.setScale(new Vector(new float[]{0.2f,0.2f,0.2f}));
        pot.setTickObj(tempRot);
        pot.mesh.initOpenGLMeshData();


//        Model city = ModelBuilder.buildModelFromFile("desertCity.obj",meshInstances);
//        city.setScale(1f,1f,1f);
//        city.mesh.displayMeshInformation();
//        city.mesh.initOpenGLMeshData();

//        models.add(city);
        models.add(deer);
        models.add(mill);
        models.add(deer2);

        modelsOldRenderMethod.add(grid);
//		modelsOldRenderMethod.add(pot);

    }

    public void initPauseScreen() {

        int width = 200;
        int height = 100;

        //		Making Exit button
        EXIT = new GUI.ButtonLWJGL(this,new Vector(new float[]{0.05f,0.1f}),width,height);
        EXIT.text = "EXIT";

        ButtonLWJGL.Behaviour exitButtonBehaviour = (b, mp, isPressed) -> {

            if(b.isMouseInside(mp)) {
                b.textColor = Color.RED;
                if(isPressed) {
                    System.out.println("Exit pressed");
                    programRunning = false;
                }
            }
            else {
                b.textColor = Color.LIGHT_GRAY;
            }
        };

        EXIT.bgColor = Color.DARK_GRAY;
        EXIT.behaviour = exitButtonBehaviour;

//		Font f = Font.getFont("Arial").deriveFont(Font.BOLD,24);
        EXIT.textFont = new Font("Sans-Serif",Font.BOLD,20);


//		Making FullScreen Toggle
        FULLSCREEN = new GUI.ButtonLWJGL(this,new Vector(new float[]{0.05f,0.25f}),width,height);
        FULLSCREEN.text = "FULLSCREEN";

        ButtonLWJGL.Behaviour fullscreenBehaviour = (b,mp,isPressed) -> {

            if(b.isMouseInside(mp)) {
                b.textColor = Color.RED;
                if(isPressed && getDisplay().displayMode != DisplayLWJGL.DisplayMode.FULLSCREEN) {
                    getDisplay().displayMode = DisplayLWJGL.DisplayMode.FULLSCREEN;
                    getDisplay().startScreen();
                }
            }
            else {
                b.textColor = Color.LIGHT_GRAY;
            }

        };

        FULLSCREEN.setBehaviour(fullscreenBehaviour);
        FULLSCREEN.bgColor = Color.DARK_GRAY;
        FULLSCREEN.textFont = new Font("Consolas", Font.BOLD,20);

//		Making WindowedMode Toggle
        WINDOWED = new GUI.ButtonLWJGL(this,new Vector(new float[]{0.05f,0.4f}),width,height);
        WINDOWED.text = "WINDOWED MODE";

        ButtonLWJGL.Behaviour windowedBehaviour = (b,mp,isPressed) -> {

            if(b.isMouseInside(mp)) {
                b.textColor = Color.RED;
                if(isPressed && getDisplay().displayMode != DisplayLWJGL.DisplayMode.WINDOWED) {
                    getDisplay().displayMode = DisplayLWJGL.DisplayMode.WINDOWED;
                    getDisplay().startScreen();
                }
            }
            else {
                b.textColor = Color.LIGHT_GRAY;
            }

        };

        WINDOWED.setBehaviour(windowedBehaviour);
        WINDOWED.bgColor = Color.DARK_GRAY;
        WINDOWED.textFont = new Font("Consolas", Font.BOLD,20);

        pauseButtons.add(EXIT);
        pauseButtons.add(FULLSCREEN);
        pauseButtons.add(WINDOWED);
    }

    public void initInputControls() {

    }

    public void runGame() {

        double dt = 0.0;
        double startTime = System.nanoTime();
        double currentTime = System.nanoTime();
        double timerStartTime = System.nanoTime();
        double timer = 0.0;
        double tempDt = 0;
        float tickInterval = 0;

        while (programRunning) {

            double timeU = ((1000000000.0 / targetFPS));
            currentTime = System.nanoTime();
            tempDt = (currentTime - startTime);
            dt += tempDt/timeU;
            tickInterval += tempDt;
            startTime = currentTime;
            timer = (currentTime - timerStartTime);

            if (dt >= 1) {
                speedConstant = (float) (tickInterval /1000000000.0);
                tickInterval = 0;
                tick();
                render();
                fps++;
                dt = 0;
            }

            if (timer >= 1000000000.0) {
                displayFPS = fps;
                fps = 0;
                timer = 0;
                timerStartTime = System.nanoTime();
            }
        }

        cleanUp();
    }

    public void cleanUp() {
        display.cleanUp();
        renderingEngine.cleanUp();
        for(Model m:models) {
            m.mesh.cleanUp();
        }
    }

    public void tick() {
        tickInput();

        if(glfwWindowShouldClose(display.getWindow())) {
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

        models.forEach(Model::tick);
        modelsOldRenderMethod.forEach(Model::tick);

        if(!isGameRunning) {
            pauseButtons.forEach((b) -> b.tick(mousePos,input.isLeftMouseButtonPressed));
        }
        else {
            calculate3DCamMovement();
            cam.tick();
        }

    }

    public void tickInput() {

        if(input.keyDown(GLFW_KEY_W)) {
            float cameraSpeed = speed * speedConstant * speedMultiplier;
            Vector[] rotationMatrix = cam.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector x = rotationMatrix[0];
            Vector y = new Vector(new float[] {0,1,0});
            Vector z = x.cross(y);
            cam.setPos(cam.getPos().sub(z.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(GLFW_KEY_S)) {
            float cameraSpeed = speed * speedConstant * speedMultiplier;
            Vector[] rotationMatrix = cam.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector x = rotationMatrix[0];
            Vector y = new Vector(new float[] {0,1,0});
            Vector z = x.cross(y);
            cam.setPos(cam.getPos().add(z.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(GLFW_KEY_A)) {
            float cameraSpeed = speed * speedConstant * speedMultiplier;
            Vector[] rotationMatrix = cam.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector v = rotationMatrix[0];
            cam.setPos(cam.getPos().sub(v.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(GLFW_KEY_D)) {
            float cameraSpeed = speed * speedConstant * speedMultiplier;
            Vector[] rotationMatrix = cam.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector v = rotationMatrix[0];
            cam.setPos(cam.getPos().add(v.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(GLFW_KEY_SPACE)) {
            float cameraSpeed = speed * speedConstant * speedMultiplier;

            Vector v = new Vector(new float[] {0,1,0});
            cam.setPos(cam.getPos().add(v.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(GLFW_KEY_LEFT_SHIFT)) {
            float cameraSpeed = speed * speedConstant * speedMultiplier;

            Vector v = new Vector(new float[] {0,1,0});
            cam.setPos(cam.getPos().sub(v.scalarMul(cameraSpeed)));
        }

        if(input.keyDownOnce(GLFW_KEY_ESCAPE)) {
            isGameRunning = !isGameRunning;
        }

        if(isGameRunning) {
            if(input.keyDownOnce(GLFW_KEY_R)) {
//              cam.setPos(models.get(lookAtIndex).getCentre());
                cam.lookAtModel(models.get(lookAtIndex));
            }

            if(input.keyDownOnce(GLFW_KEY_LEFT_CONTROL)) {
                if(speedMultiplier == 1) speedMultiplier = speedIncreaseMultiplier;
                else speedMultiplier = 1;
            }

            if(input.keyDownOnce(GLFW_KEY_F)) {
                if(targetFPS == display.getRefreshRate()) {
                    targetFPS = 10000;
                }
                else {
                    targetFPS = display.getRefreshRate();
                }
            }

            if(input.keyDownOnce(GLFW_KEY_Q)) {
                if(renderingEngine.getRenderPipeline() == RenderingEngineLWJGL.RenderPipeline.Quat) renderingEngine.setRenderPipeline(RenderingEngineLWJGL.RenderPipeline.Matrix);
                else renderingEngine.setRenderPipeline(RenderingEngineLWJGL.RenderPipeline.Quat);
            }

            if(input.keyDownOnce(GLFW_KEY_V)) {
                display.toggleWindowModes();
            }
        }
    }

    public void calculate3DCamMovement() {
        if (mouseDelta.getNorm() != 0 && isGameRunning) {

            float yawIncrease   = mouseXSensitivity * speedConstant * -mouseDelta.get(0);
            float pitchIncrease = mouseYSensitivity * speedConstant * -mouseDelta.get(1);

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

    public RenderingEngineLWJGL getRenderingEngine() {
        return renderingEngine;
    }

    public void render() {
        renderingEngine.render3(models);

        glfwSwapBuffers(display.getWindow());
        glfwPollEvents();
        input.poll();
    }

    public List<Model> getModels() {
        return models;
    }

    public void setModels(List<Model> models) {
        this.models = models;
    }

    public double getTargetFPS() {
        return targetFPS;
    }

    public void setTargetFPS(double targetFPS) {
        this.targetFPS = targetFPS;
    }

    public boolean isShouldFPS() {
        return shouldDisplayFPS;
    }

    public void setShouldFPS(boolean shouldFPS) {
        this.shouldDisplayFPS = shouldFPS;
    }

    public boolean isProgramRunning() {
        return programRunning;
    }

    public void setProgramRunning(boolean programRunning) {
        this.programRunning = programRunning;
    }

    public DisplayLWJGL getDisplay() {
        return display;
    }

    public CameraLWJGL getCamera() {
        return cam;
    }

}
