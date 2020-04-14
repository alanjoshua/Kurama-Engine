package ENED_Simulation;

import engine.DataStructure.Mesh.Mesh;
import engine.Math.Matrix;
import engine.Math.Quaternion;
import engine.Math.Vector;
import engine.inputs.Input;
import engine.model.Model;

import java.util.Optional;

public class Robot extends Model {

    private Simulation game;

    public Vector translationDirection;
    public float rotationSpeed = 150;
    public float movementSpeed = 10;
    public boolean isManualControl = false;
    private Input input;
    public float scanRadius = 5;
    private Box boxPicked;

    public Robot(Simulation game,Mesh mesh, String identifier) {
        super(mesh, identifier);
        this.game = game;
        input = game.getInput();
        translationDirection = new Vector(3,0);
    }

    @Override
    public void tick(ModelTickInput params) {
        translationDirection = new Vector(3,0);

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

        if(input.keyDown(input.ONE)) {
            if(boxPicked != null) {
                boxPicked.setPos(new Vector(new float[]{this.pos.get(0), 0, this.getPos().get(2)}));
                boxPicked = null;
            }
        }

        if(!isManualControl) {
            autoMove();
        }

       translationDirection = translationDirection.normalise();

        if(boxPicked == null) {
            Matrix robotMatrix = this.getOrientation().getRotationMatrix();
            Optional<Box> optional = game.boxes
                    .stream()
                    .filter(b -> b.isRobotInCorrectPositionToScan(this, robotMatrix))
                    .findFirst();
            if (optional.isPresent()) {
                boxPicked = optional.get();
                System.out.println("box to be scanned is: " + boxPicked.identifier);
                System.out.print("barcode: ");
                boxPicked.barCode.display();
            }
        }
        else {   //Already picked a box
            boxPicked.setPos(this.pos.add(new Vector(new float[]{0,2,0})));
            boxPicked.setOrientation(this.getOrientation());

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

    public boolean updatePosAfterBoundingBoxCheck(Vector newPos) {
        if(newPos.get(0) >= 0 && newPos.get(0) < game.simWidth && newPos.get(2) <= 0 && newPos.get(2) > -game.simDepth && !game.isModelColliding(this)) {
            this.pos = newPos;
            return true;
        }
        return false;
    }

    public void moveForward(ModelTickInput params) {
        Vector[] rotationMatrix = getOrientation().getRotationMatrix().convertToColumnVectorArray();

        Vector x = rotationMatrix[0];
        Vector y = new Vector(new float[] {0,1,0});
        Vector z = x.cross(y);
        Vector delta = z.scalarMul(movementSpeed * params.timeDelta * -1);
        Vector newPos = getPos().add(delta);

        if(updatePosAfterBoundingBoxCheck(newPos)) {
            translationDirection = translationDirection.add(delta);
        }
    }

    public void moveBackward(ModelTickInput params) {
        Vector[] rotationMatrix = getOrientation().getRotationMatrix().convertToColumnVectorArray();

        Vector x = rotationMatrix[0];
        Vector y = new Vector(new float[] {0,1,0});
        Vector z = x.cross(y);
        Vector delta = z.scalarMul(movementSpeed * params.timeDelta);
        Vector newPos = getPos().add(delta);

        if(updatePosAfterBoundingBoxCheck(newPos)) {
            translationDirection = translationDirection.add(delta);
        }
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
