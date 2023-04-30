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

    public int framesAlive = 0;
    public int framesTakenToLand = 0;
    public NeuralNetwork brain; // neuralNetwork
    public float[] chromosome;
    public boolean hasRunEnded = false;
    public boolean landedSuccessfully = false;
    public float fitness = 0;
    public Renderable selfRenderableReference;
    public LunarLanderGame gameHandler;
    private Vector xAxis = new Vector(1,0,0);
    private Vector yAxis = new Vector(0,1,0);

    public UFO(Game game, List<Mesh> meshes, float[] chromosome, String identifier) {
        super(game, meshes, identifier);
        gameHandler = (LunarLanderGame) game;
        this.chromosome = chromosome;

        brain = new NeuralNetwork(gameHandler.layers, chromosome);
    }


    public float[] process() {
        if(!hasRunEnded) {
            var pitchYawRoll = orientation.getPitchYawRoll();

            var nnInput = new Vector(new float[]{pos.get(0), pos.get(1), pitchYawRoll.get(0)/180f, pitchYawRoll.get(1)/180f, pitchYawRoll.get(2)});
            var brainOutput = brain.runBrain(nnInput);

            var isUp = brainOutput.get(0) >= 0.5 ? true: false;
            var isLeft = brainOutput.get(1) >= brainOutput.get(2) ? true: false;
            var isRight = brainOutput.get(1) < brainOutput.get(2) ? true: false;

            // Use the neural network to decide how to move the ufo
            processMovement(isUp, isLeft, isRight);
            framesAlive++;
        }

        return null;
    }

    public void processMovement(boolean up, boolean left, boolean right) {

        if(up) {
            Vector[] rotationMatrix = getOrientation().getRotationMatrix().convertToColumnVectorArray();

            Vector x = rotationMatrix[0];
            Vector y = new Vector(new float[] {0,0,1});
            Vector dir = x.cross(y).normalise();

            acceleration = dir.scalarMul(-gameHandler.thrustAccel);
        }
        else {
            acceleration = new Vector(0f, 0, 0f);
        }

        var angle = orientation.getPitchYawRoll();

        if(left && !right) {
            orientation = orientation.multiply(Quaternion.getQuaternionFromEuler(0, 0, gameHandler.turnVel));

            if(angle.get(0) != 0f) {
                orientation = orientation.multiply(Quaternion.getAxisAsQuat(0,0,1,-5));
            }

        }

        if(right && !left) {
            orientation = orientation.multiply(Quaternion.getQuaternionFromEuler(0, 0, -gameHandler.turnVel));

            if(angle.get(0) != 0f) {
                orientation = orientation.multiply(Quaternion.getAxisAsQuat(0,0,1,5));
            }
        }

        acceleration = acceleration.add(new Vector(0f, -gameHandler.gravity, 0f));
        velocity = velocity.add(acceleration);
        pos = pos.add(velocity);

        selfRenderableReference.isDirty = true;

        float curAngle = orientation.getAngleOfRotation();
        var angleWithXAxis = Math.toRadians(xAxis.getAngleBetweenVectors(velocity));
        var horizontalVel = velocity.getNorm() * Math.sin(angleWithXAxis);
        var verticalVel =  velocity.getNorm() * Math.cos(angleWithXAxis);
        var vertDirection = yAxis.dot(velocity.normalise());

        fitness += 2f; // points for staying alive

        if(horizontalVel < winMaxSpeed) {
            fitness += 1;
        }

        if(verticalVel < winMaxSpeed) {
            fitness += 1;
        }

        if(curAngle < winMaxAngle && verticalVel < winMaxSpeed) {
            fitness += 2;
        }

        if(curAngle < winMaxAngle && verticalVel < winMaxSpeed && horizontalVel < winMaxSpeed) {
            fitness += 5;
        }

        float distFromGround = (pos.get(1) - gameHandler.yBottom);
        fitness += Math.min(2f, (gameHandler.yTop - gameHandler.yBottom)/distFromGround);

        fitness += Math.min(2f, 90f/(90f-Math.max(curAngle, 0.01f)));

        // Check for win or lose conditions
        if(pos.get(1) < gameHandler.yBottom) {

            if(curAngle < winMaxAngle && horizontalVel < winMaxSpeed && (vertDirection >= 0 || verticalVel < winMaxSpeed)) {
                landedSuccessfully = true;
                framesTakenToLand = framesAlive;
                fitness += 10000 + (gameHandler.framesPerRun - framesTakenToLand) * 100f;
            }
            else {
                fitness-= 500;
            }

            hasRunEnded = true;
            pos.setDataElement(1, gameHandler.yBottom);
            velocity = new Vector(0,0,0);
        }

        if(pos.get(1) > gameHandler.yTop) {
            fitness -= 1000;
            hasRunEnded = true;
        }

        if(pos.get(0) > gameHandler.xRight) {
            fitness -= 1000;
            hasRunEnded = true;
        }

        if(pos.get(0) < gameHandler.xLeft) {
            fitness -= 1000;
            hasRunEnded = true;
        }

    }

}
