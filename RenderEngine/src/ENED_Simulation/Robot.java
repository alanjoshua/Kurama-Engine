package ENED_Simulation;

import engine.DataStructure.Mesh.Mesh;
import engine.Math.Quaternion;
import engine.Math.Vector;
import engine.game.Game;
import engine.inputs.Input;
import engine.model.Model;
import engine.model.ModelBuilder;

public class Robot extends Model {

    private Game game;

    public float rotationSpeed = 150;
    public float movementSpeed = 10;

    public Robot(Game game,Mesh mesh, String identifier) {
        super(mesh, identifier);
        this.game = game;
    }

    @Override
    public void tick(ModelTickInput params) {
        Input input = game.getInput();

        if(input.keyDown(input.UP_ARROW)) {
            moveForward(params);
        }

        if(input.keyDown(input.DOWN_ARROW)) {
            moveBackward(params);
        }

        if(input.keyDown(input.LEFT_ARROW)) {
           turnLeft(params);
        }

        if(input.keyDown(input.RIGHT_ARROW)) {
            turnRight(params);
        }

    }

    public void moveForward(ModelTickInput params) {
        Vector[] rotationMatrix = getOrientation().getRotationMatrix().convertToColumnVectorArray();

        Vector x = rotationMatrix[0];
        Vector y = new Vector(new float[] {0,1,0});
        Vector z = x.cross(y);
        this.pos = getPos().sub(z.scalarMul(movementSpeed * params.timeDelta));
    }

    public void moveBackward(ModelTickInput params) {
        Vector[] rotationMatrix = getOrientation().getRotationMatrix().convertToColumnVectorArray();

        Vector x = rotationMatrix[0];
        Vector y = new Vector(new float[] {0,1,0});
        Vector z = x.cross(y);
        this.pos = getPos().add(z.scalarMul(movementSpeed * params.timeDelta));
    }

    public void turnLeft(ModelTickInput params) {
        Quaternion rot = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}), rotationSpeed* params.timeDelta);
        Quaternion newQ = rot.multiply(getOrientation());
        setOrientation(newQ);
    }

    public void turnRight(ModelTickInput params) {
        Quaternion rot = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}), -rotationSpeed* params.timeDelta);
        Quaternion newQ = rot.multiply(getOrientation());
        setOrientation(newQ);
    }

}
