package ENED_Simulation;

import engine.DataStructure.Mesh.Mesh;
import engine.Math.Quaternion;
import engine.Math.Vector;
import engine.game.Game;
import engine.inputs.Input;
import engine.model.Model;
import engine.model.ModelBuilder;
import org.lwjgl.system.CallbackI;

public class Robot extends Model {

    private Simulation game;

    public float rotationSpeed = 150;
    public float movementSpeed = 10;
    private boolean isManualControl = false;

    public Robot(Simulation game,Mesh mesh, String identifier) {
        super(mesh, identifier);
        this.game = game;
    }

    @Override
    public void tick(ModelTickInput params) {
        Input input = game.getInput();
        isManualControl = false;

        //pos.display();

        if(input.keyDown(input.UP_ARROW)) {
            moveForward(params);
            isManualControl = true;
        }

        if(input.keyDown(input.DOWN_ARROW)) {
            moveBackward(params);
            isManualControl = true;
        }

        if(input.keyDown(input.LEFT_ARROW)) {
           turnLeft(params);
            isManualControl = true;
        }

        if(input.keyDown(input.RIGHT_ARROW)) {
            turnRight(params);
            isManualControl = true;
        }

        if(!isManualControl) {
            autoMove();
        }

    }

    public void autoMove() {

    }

    public void IGPS(String text) {
        String[] split = text.trim().split("\\s+");

        float[] radVals = new float[3];
        for(int i = 0;i < radVals.length;i++) {
            radVals[i] = Float.parseFloat(split[i]);
        }

        radVals[2] *= -1;

        this.setPos(new Vector(radVals));
        System.out.println("coordinates logged");
    }

    public void updatePosAfterBoundingBoxCheck(Vector newPos) {
        if(newPos.get(0) >= 0 && newPos.get(0) < game.simWidth && newPos.get(2) <= 0 && newPos.get(2) > -game.simDepth && !game.isModelColliding(this)) {
            this.pos = newPos;
        }
    }

    public void moveForward(ModelTickInput params) {
        Vector[] rotationMatrix = getOrientation().getRotationMatrix().convertToColumnVectorArray();

        Vector x = rotationMatrix[0];
        Vector y = new Vector(new float[] {0,1,0});
        Vector z = x.cross(y);
        Vector newPos = getPos().sub(z.scalarMul(movementSpeed * params.timeDelta));
        updatePosAfterBoundingBoxCheck(newPos);
    }

    public void moveBackward(ModelTickInput params) {
        Vector[] rotationMatrix = getOrientation().getRotationMatrix().convertToColumnVectorArray();

        Vector x = rotationMatrix[0];
        Vector y = new Vector(new float[] {0,1,0});
        Vector z = x.cross(y);
        Vector newPos = getPos().add(z.scalarMul(movementSpeed * params.timeDelta));
        updatePosAfterBoundingBoxCheck(newPos);
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
