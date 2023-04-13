package main;

import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.components.model.Model;
import Kurama.ComponentSystem.components.model.SceneComponent;
import Kurama.Math.Quaternion;
import Kurama.Math.Vector;
import Kurama.Mesh.Mesh;
import Kurama.Vulkan.Renderable;
import Kurama.game.Game;

import java.util.List;

public class UFO extends Model {

    public long startTime;
    public Object brain; // neuralNetwork
    public Object chromosome;
    public float numFramesRun = 0;
    public boolean hasRunEnded = false;
    public float fitness;
    public Renderable selfRenderableReference;

    public LunarLanderGame gameHandler;

    public UFO(Game game, List<Mesh> meshes, String identifier) {
        super(game, meshes, identifier);
        gameHandler = (LunarLanderGame) game;
    }


    public float[] process(Object frameData, float timeDelta) {
        if(!hasRunEnded) {
            var in = gameHandler.input;
            numFramesRun++;
            // Use the neural network to decide how to move the ufo
            processMovement(in.keyDown(in.UP_ARROW), in.keyDown(in.LEFT_ARROW), in.keyDown(in.RIGHT_ARROW), timeDelta);
        }
        else {

        }
        return null;
    }

    public void processMovement(boolean up, boolean left, boolean right, float timeDelta) {

        if(up) {
            Vector[] rotationMatrix = getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector x = rotationMatrix[0];
            Vector y = new Vector(new float[] {0,0,1});
            Vector dir = x.cross(y).normalise();

            acceleration = dir.scalarMul(-gameHandler.thrustAccel * timeDelta);
        }
        else {
            acceleration = new Vector(0f, 0, 0f);
        }

        float curAngle = orientation.getAngleOfRotation();
        if(left) {
            if(curAngle < 85f) {
                orientation = orientation.multiply(Quaternion.getQuaternionFromEuler(0, 0, gameHandler.turnVel * timeDelta));
            }
            else {
                orientation = orientation.multiply(Quaternion.getAxisAsQuat(0,0,1,-5));
            }
        }

        if(right) {
            if(curAngle < 85f) {
                orientation = orientation.multiply(Quaternion.getQuaternionFromEuler(0, 0, -gameHandler.turnVel * timeDelta));
            }
            else {
                orientation = orientation.multiply(Quaternion.getAxisAsQuat(0,0,1,5));
            }
        }

        acceleration = acceleration.add(new Vector(0f, -gameHandler.gravity*timeDelta, 0f));
        velocity = velocity.add(acceleration);
        pos = pos.add(velocity);
        System.out.println(pos);
        selfRenderableReference.isDirty = true;

        if(pos.get(1) < gameHandler.yBottom) {
            pos.setDataElement(1, gameHandler.yBottom);
            velocity = new Vector(0,0,0);
            hasRunEnded = true;
        }

        if(pos.get(1) > gameHandler.yTop) {
            pos.setDataElement(1, gameHandler.yTop - 0.2f);
            velocity = new Vector(0,0,0);
            hasRunEnded = true;
        }

        if(pos.get(0) > gameHandler.xRight) {
            pos.setDataElement(0, gameHandler.xRight - 0.2f);
            velocity = new Vector(0,0,0);
            hasRunEnded = true;
        }

        if(pos.get(0) < gameHandler.xLeft) {
            pos.setDataElement(0, gameHandler.xLeft + 0.2f);
            velocity = new Vector(0,0,0);
            hasRunEnded = true;
        }

    }

}
