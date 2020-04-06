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
import models.DataStructure.Mesh.MeshLWJGL;
import models.Model;
import models.ModelLWJGL;
import models.Model.Tick;
import models.ModelBuilder;
import rendering.Camera;
import rendering.RenderingEngineLWJGL;

import org.lwjgl.glfw.*;

import static org.lwjgl.glfw.GLFW.*;

import org.lwjgl.opengl.*;

public class GameLWJGL extends Game implements Runnable {

    protected List<GUI.ButtonLWJGL> pauseButtons;
    protected ButtonLWJGL EXIT;
    protected ButtonLWJGL FULLSCREEN;
    protected ButtonLWJGL WINDOWED;

    protected Vector mouseDelta;
    protected Vector mousePos;

    protected boolean prevGameState = false;

    Map<String, MeshLWJGL> meshInstances;

    public GameLWJGL(String threadName) {
        super(threadName);
    }

    public void start() {
        String osName = System.getProperty("os.name");
        if ( osName.contains("Mac") ) {
            gameLoopThread.run();   //To make this program compatible with macs
        } else {
            System.out.println("start called");
            gameLoopThread.start();
        }
    }

    public void init() {
        meshInstances = new HashMap<>();
        renderingEngine = new RenderingEngineLWJGL(this);
        display = new DisplayLWJGL(this);
        display.startScreen();
        renderingEngine.init();

        input = new InputLWJGL(((DisplayLWJGL)display).getWindow());

        pauseButtons = new ArrayList<>();
        models = new ArrayList<>();

        cam = new Camera(this,null,null,null, new Vector(new float[] {0,7,5}),90, 0.001f, 1000,
                display.getWidth(), display.getHeight());

        initModels();
        initPauseScreen();

        cam.updateValues();
        cam.lookAtModel(models.get(lookAtIndex));

        targetFPS = ((DisplayLWJGL)display).getRefreshRate();

    }

    public void initModels() {
        Tick tempRot = (m -> {
            Quaternion rot = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}), 50*speedConstant);
            Quaternion newQ = rot.multiply(m.getOrientation());
            m.setOrientation(newQ);
        });

        Model deer = ModelBuilder.buildModelLWJGLFromFile("deer.obj",meshInstances);
        deer.setPos(new Vector(new float[] {-10,15,-15}));
        deer.setScale(new Vector(new float[] { 0.01f, 0.01f, 0.01f }));

//        ModelLWJGL deer2 = ModelBuilder.buildModelLWJGLFromFile("deer.obj",meshInstances);
//        deer2.setPos(new Vector(new float[] {0,18,0}));
//        deer2.setScale(new Vector(new float[] { 0.01f, 0.01f, 0.01f }));
//
        Model mill = ModelBuilder.buildModelLWJGLFromFile("low-poly-mill.obj",meshInstances);
        mill.setPos(new Vector(new float[] {10,5,0}));
        mill.setScale(new Vector(new float[] { 0.5f, 0.5f, 0.5f }));
////
//        ModelLWJGL pot = ModelBuilder.buildModelLWJGLFromFile("TeapotHex3.obj",meshInstances);
//        pot.setPos(new Vector(new float[]{0,10,10}));
//        pot.setScale(new Vector(new float[]{0.2f,0.2f,0.2f}));
//        pot.setTickObj(tempRot);
//
//
//        ModelLWJGL ironMan = ModelBuilder.buildModelLWJGLFromFile("IronMan.obj",meshInstances);
//        ironMan.setScale(1f,1f,1f);
//        ironMan.setTickObj(tempRot);

        Model sasuke = ModelBuilder.buildModelLWJGLFromFile("Sasuke.obj",meshInstances);
        sasuke.setScale(0.1f);
        sasuke.setPos(0,17,0);
        sasuke.setTickObj(tempRot);

//        models.add(ironMan);
        models.add(sasuke);
        models.add(deer);
        models.add(mill);
//        models.add(pot);

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
                if(isPressed && getDisplay().displayMode != Display.DisplayMode.FULLSCREEN) {
                    getDisplay().displayMode = Display.DisplayMode.FULLSCREEN;
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

    public void cleanUp() {
        display.cleanUp();
        renderingEngine.cleanUp();
        for(Model m:models) {
            m.mesh.cleanUp();
        }
    }

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

        models.forEach(Model::tick);

        if(!isGameRunning) {
            pauseButtons.forEach((b) -> b.tick(mousePos,((InputLWJGL)input).isLeftMouseButtonPressed));
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

    public void render() {
        ((RenderingEngineLWJGL)renderingEngine).render(models);

        glfwSwapBuffers(((DisplayLWJGL)display).getWindow());
        glfwPollEvents();
        input.poll();
    }

}
