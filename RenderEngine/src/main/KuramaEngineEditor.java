package main;

import engine.DataStructure.Mesh.Mesh;
import engine.Math.Quaternion;
import engine.Math.Vector;
import engine.camera.Camera;
import engine.display.Display;
import engine.display.DisplayLWJGL;
import engine.game.Game;
import engine.inputs.Input;
import engine.inputs.InputLWJGL;
import engine.lighting.DirectionalLight;
import engine.lighting.SpotLight;
import engine.model.Model;
import engine.renderingEngine.RenderingEngine;
import engine.renderingEngine.RenderingEngineGL;

import java.util.List;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;

public class KuramaEngineEditor extends Game implements Runnable {

    protected DisplayLWJGL display;
    protected Camera cam;
    protected InputLWJGL input;
    protected RenderingEngineGL renderingEngine;

    protected float mouseXSensitivity = 20f;
    protected float mouseYSensitivity = 20f;
    protected float speed = 15f;
    protected float speedMultiplier = 1;
    protected float speedIncreaseMultiplier = 2;

    protected Vector mouseDelta;
    protected Vector mousePos;

    protected boolean prevGameState = false;
    protected boolean isGameRunning = true;


    public KuramaEngineEditor(String threadName) {
        super(threadName);
    }

    @Override
    public void init() {

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
        hud.tick();

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

        if(isGameRunning) {
        Model.ModelTickInput params = new Model.ModelTickInput();
        params.timeDelta = timeDelta;
        scene.updateAllModels(params);
//        scene.models.forEach(m -> m.tick(params));
        }

    }


//  User input controls
    public void tickInput() {

        Vector posDelta = new Vector(3,0);

        if(input.keyDown(input.W)) {
            float cameraSpeed = speed * timeDelta * speedMultiplier;
            Vector[] rotationMatrix = cam.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector x = rotationMatrix[0];
            Vector y = new Vector(new float[] {0,1,0});
            Vector z = x.cross(y);
            posDelta = posDelta.add((z.scalarMul(-cameraSpeed)));
//            cam.setPos(cam.getPos().sub(z.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(input.S)) {
            float cameraSpeed = speed * timeDelta * speedMultiplier;
            Vector[] rotationMatrix = cam.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector x = rotationMatrix[0];
            Vector y = new Vector(new float[] {0,1,0});
            Vector z = x.cross(y);
            posDelta = posDelta.add(z.scalarMul(cameraSpeed));
//            cam.setPos(cam.getPos().add(z.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(input.A)) {
            float cameraSpeed = speed * timeDelta * speedMultiplier;
            Vector[] rotationMatrix = cam.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector v = rotationMatrix[0];
            posDelta = posDelta.add(v.scalarMul(-cameraSpeed));
//            cam.setPos(cam.getPos().sub(v.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(input.D)) {
            float cameraSpeed = speed * timeDelta * speedMultiplier;
            Vector[] rotationMatrix = cam.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector v = rotationMatrix[0];
            posDelta = posDelta.add(v.scalarMul(cameraSpeed));
//            cam.setPos(cam.getPos().add(v.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(input.SPACE)) {
            float cameraSpeed = speed * timeDelta * speedMultiplier;

            Vector v = new Vector(new float[] {0,1,0});
            posDelta = posDelta.add(v.scalarMul(cameraSpeed));
//            cam.setPos(cam.getPos().add(v.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(input.LEFT_SHIFT)) {
            float cameraSpeed = speed * timeDelta * speedMultiplier;

            Vector v = new Vector(new float[] {0,1,0});
            posDelta = posDelta.add(v.scalarMul(-cameraSpeed));
//            cam.setPos(cam.getPos().sub(v.scalarMul(cameraSpeed)));
        }

        if(input.keyDownOnce(input.ESCAPE)) {
            isGameRunning = !isGameRunning;
        }

        if(input.keyDown(input.UP_ARROW)) {
            DirectionalLight light = scene.directionalLights.get(0);
            scene.directionalLights.get(0).setPos(light.getPos().add(new Vector(0,timeDelta* 3,0)));
        }
        if(input.keyDown(input.DOWN_ARROW)) {
            var light = scene.directionalLights.get(0);
            scene.directionalLights.get(0).setPos(light.getPos().sub(new Vector(0,timeDelta* 3,0)));
        }

        if(isGameRunning) {

            if(input.keyDownOnce(input.LEFT_CONTROL)) {
                if(speedMultiplier == 1) speedMultiplier = speedIncreaseMultiplier;
                else speedMultiplier = 1;
            }

            if(input.keyDownOnce(input.F)) {
                if(targetFPS == display.getRefreshRate()) {
                    targetFPS = 10000;
                }
                else {
                    targetFPS = display.getRefreshRate();
                }
                System.out.println("Changed target resolution"+targetFPS);
            }

            if(input.keyDownOnce(input.V)) {
                display.toggleWindowModes();
            }
        }

        Vector newPos = cam.getPos().add(posDelta);
        cam.setPos(newPos);
    }

//    Calculating camera direction movement
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

    }

    @Override
    public RenderingEngine getRenderingEngine() {
        return null;
    }

    @Override
    public Display getDisplay() {
        return null;
    }

    @Override
    public Camera getCamera() {
        return null;
    }

    @Override
    public Input getInput() {
        return null;
    }

}
