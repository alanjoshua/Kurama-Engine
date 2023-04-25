package main;

import Kurama.ComponentSystem.components.model.Model;
import Kurama.Math.Quaternion;
import Kurama.Math.Vector;
import Kurama.Mesh.Mesh;
import Kurama.Vulkan.Renderable;
import Kurama.game.Game;

import java.util.List;

import static Kurama.utils.Logger.log;
import static Kurama.utils.Logger.logPerSec;
import static main.LunarLanderGame.winMaxAngle;
import static main.LunarLanderGame.winMaxSpeed;

public class UFO extends Model {

    public long timeAlive = 0;
    public long timeTakenToLand = 0;
    public NeuralNetwork brain; // neuralNetwork
    public Object chromosome;
    public boolean hasRunEnded = false;
    public boolean landedSuccessfully = false;
    public float fitness = 0;
    public Renderable selfRenderableReference;
    public LunarLanderGame gameHandler;
    private Vector xAxis = new Vector(1,0,0);
    private Vector yAxis = new Vector(0,1,0);

    public UFO(Game game, List<Mesh> meshes, String identifier) {
        super(game, meshes, identifier);
        gameHandler = (LunarLanderGame) game;

        brain = new NeuralNetwork(gameHandler.layers);
    }


    public float[] process(Object frameData, float timeDelta) {
        if(!hasRunEnded) {

            var orient = orientation.getCoordinate();
            var nnInput = new Vector(new float[]{pos.get(0), pos.get(1), orient.get(0), orient.get(1), orient.get(2), orient.get(3)});
            var brainOutput = brain.runBrain(nnInput);

            var isUp = brainOutput.get(0) >= 0.5 ? true: false;
            var isLeft = brainOutput.get(1) >= brainOutput.get(2) ? true: false;
            var isRight = brainOutput.get(1) < brainOutput.get(2) ? true: false;

            // Use the neural network to decide how to move the ufo
            processMovement(isUp, isLeft, isRight, timeDelta);
            timeAlive+=timeDelta;
        }

        return null;
    }

    public float calcFinalFitness() {
        if(landedSuccessfully) {
            fitness = 1000f;

            fitness += (gameHandler.timePerRun / timeTakenToLand) * 100;
        }
        else {
            fitness = (timeAlive / gameHandler.timePerRun) * 500f;
        }

        return fitness;
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

        if(left) {
//            if(curAngle < 85f) {
                orientation = orientation.multiply(Quaternion.getQuaternionFromEuler(0, 0, gameHandler.turnVel * timeDelta));
//            }
//            else {
//                orientation = orientation.multiply(Quaternion.getAxisAsQuat(0,0,1,-5));
//            }
        }

        else if(right) {
//            if(curAngle < 85f) {
                orientation = orientation.multiply(Quaternion.getQuaternionFromEuler(0, 0, -gameHandler.turnVel * timeDelta));
//            }
//            else {
//                orientation = orientation.multiply(Quaternion.getAxisAsQuat(0,0,1,5));
//            }
        }

        acceleration = acceleration.add(new Vector(0f, -gameHandler.gravity*timeDelta, 0f));
        velocity = velocity.add(acceleration);
        pos = pos.add(velocity);

        selfRenderableReference.isDirty = true;

        if(pos.get(1) < gameHandler.yBottom) {

            var angleWithXAxis = Math.toRadians(xAxis.getAngleBetweenVectors(velocity));
            var horizontalVel = velocity.getNorm() * Math.sin(angleWithXAxis);
            var verticalVel =  velocity.getNorm() * Math.cos(angleWithXAxis);
            var vertDirection = yAxis.dot(velocity.normalise());

            float curAngle = orientation.getAngleOfRotation();

            if(curAngle < winMaxAngle && horizontalVel < winMaxSpeed && (vertDirection >= 0 || verticalVel < winMaxSpeed)) {
                landedSuccessfully = true;
                timeTakenToLand = timeAlive;
            }

            hasRunEnded = true;
            pos.setDataElement(1, gameHandler.yBottom);
            velocity = new Vector(0,0,0);
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
