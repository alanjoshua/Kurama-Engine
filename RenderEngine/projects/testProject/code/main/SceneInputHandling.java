package main;

import Kurama.ComponentSystem.automations.Automation;
import Kurama.ComponentSystem.components.Component;
import Kurama.Math.Quaternion;
import Kurama.Math.Vector;
import Kurama.inputs.Input;
import Kurama.ComponentSystem.components.model.AnimatedModel;
import Kurama.utils.Logger;

public class SceneInputHandling implements Automation {

    public GameLWJGL game;

    public SceneInputHandling(GameLWJGL game) {
        this.game = game;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        Vector velocity = new Vector(3,0);

        if(input.keyDown(input.W)) {
            float cameraSpeed = game.speed * game.speedMultiplier;
            Vector[] rotationMatrix = game.playerCamera.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector x = rotationMatrix[0];
            Vector y = new Vector(new float[] {0,1,0});
            Vector z = x.cross(y);
            velocity = velocity.add((z.scalarMul(-cameraSpeed)));
        }

        if(input.keyDownOnce(input.ESCAPE)) {
            current.isKeyInputFocused = false;
        }

        if (input.keyDownOnce(input.F)) {
            if (game.targetFPS == game.rootGuiComponent.getRefreshRate()) {
                game.targetFPS = 10000;
            } else {
                game.targetFPS = game.rootGuiComponent.getRefreshRate();
            }
            Logger.log("Changed target resolution" + game.targetFPS);
        }

        if (input.keyDownOnce(input.V)) {
            game.rootGuiComponent.toggleWindowModes();
        }

        if(input.keyDownOnce(input.B)) {
            game.writeSceneToFile();
        }

        if(input.keyDown(input.UP_ARROW)) {
            int counter = 0;
            while(true) {
                AnimatedModel monster = (AnimatedModel) game.scene.modelID_model_map.get("monster"+counter);
                if(monster == null) {
                    break;
                }
                monster.cycleFrame(20f * (counter+1) * timeDelta);
                monster.generateCurrentSkeleton(monster.currentAnimation.currentFrame);
                counter++;
            }
            var wolf = (AnimatedModel) game.scene.modelID_model_map.get("wolf");
            wolf.cycleFrame(20f * timeDelta);
//            wolf.cycleFrame(1);
            wolf.generateCurrentSkeleton(wolf.currentAnimation.currentFrame);
        }

        if(input.keyDown(input.DOWN_ARROW)) {
            int counter = 0;
            while(true) {
                AnimatedModel monster = (AnimatedModel) game.scene.modelID_model_map.get("monster"+counter);
                if(monster == null) {
                    break;
                }
                monster.cycleFrame(-20f * (counter+1) * timeDelta);
                monster.generateCurrentSkeleton(monster.currentAnimation.currentFrame);
                counter++;
            }
            var wolf = (AnimatedModel) game.scene.modelID_model_map.get("wolf");
            wolf.cycleFrame(-20f * timeDelta);
//            wolf.cycleFrame(-1);
            wolf.generateCurrentSkeleton(wolf.currentAnimation.currentFrame);
        }

        if(input.keyDown(input.S)) {
            float cameraSpeed = game.speed * game.speedMultiplier;
            Vector[] rotationMatrix = game.playerCamera.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector x = rotationMatrix[0];
            Vector y = new Vector(new float[] {0,1,0});
            Vector z = x.cross(y);
            velocity = velocity.add(z.scalarMul(cameraSpeed));
//            cam.setPos(cam.getPos().add(z.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(input.A)) {
            float cameraSpeed = game.speed * game.speedMultiplier;
            Vector[] rotationMatrix = game.playerCamera.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector v = rotationMatrix[0];
            velocity = velocity.add(v.scalarMul(-cameraSpeed));
//            cam.setPos(cam.getPos().sub(v.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(input.D)) {
            float cameraSpeed = game.speed * game.speedMultiplier;
            Vector[] rotationMatrix = game.playerCamera.getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector v = rotationMatrix[0];
            velocity = velocity.add(v.scalarMul(cameraSpeed));
//            cam.setPos(cam.getPos().add(v.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(input.SPACE)) {
            float cameraSpeed = game.speed * game.speedMultiplier;

            Vector v = new Vector(new float[] {0,1,0});
            velocity = velocity.add(v.scalarMul(cameraSpeed));
//            cam.setPos(cam.getPos().add(v.scalarMul(cameraSpeed)));
        }

        if(input.keyDown(input.LEFT_SHIFT)) {
            float cameraSpeed = game.speed * game.speedMultiplier;

            Vector v = new Vector(new float[] {0,1,0});
            velocity = velocity.add(v.scalarMul(-cameraSpeed));
//            cam.setPos(cam.getPos().sub(v.scalarMul(cameraSpeed)));
        }

        if(input.keyDownOnce(input.LEFT_CONTROL)) {
            if(game.speedMultiplier == 1) game.speedMultiplier = game.speedIncreaseMultiplier;
            else game.speedMultiplier = 1;
        }


//        Vector newPos = scene.camera.getPos().add(posDelta);
//        scene.camera.setPos(newPos);
        game.playerCamera.velocity = velocity;

        calculate3DCamMovement();

    }

    private void calculate3DCamMovement() {
        if (game.input.getDelta().getNorm() != 0 && game.isGameRunning) {

            float yawIncrease   = game.mouseXSensitivity * game.timeDelta * -game.input.getDelta().get(0);
            float pitchIncrease = game.mouseYSensitivity * game.timeDelta * -game.input.getDelta().get(1);

            Vector currentAngle = game.playerCamera.getOrientation().getPitchYawRoll();
            float currentPitch = currentAngle.get(0) + pitchIncrease;

            if(currentPitch >= 0 && currentPitch > 60) {
                pitchIncrease = 0;
            }
            else if(currentPitch < 0 && currentPitch < -60) {
                pitchIncrease = 0;
            }

            Quaternion pitch = Quaternion.getAxisAsQuat(new Vector(new float[] {1,0,0}),pitchIncrease);
            Quaternion yaw = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}),yawIncrease);

            Quaternion q = game.playerCamera.getOrientation();

            q = q.multiply(pitch);
            q = yaw.multiply(q);
            game.playerCamera.setOrientation(q);
        }
    }

}
