package engine.model;

import engine.DataStructure.Mesh.Mesh;
import engine.Math.Quaternion;
import engine.Math.Vector;
import engine.game.Game;

public class Movable extends Model {

    public enum MOVEMENT {
        FORWARD,BACKWARD,LEFT,RIGHT
    }

    public Vector translationDirection;
    public float rotationSpeed = 150;
    public float movementSpeed = 10;
    public boolean isManualControl = false;

    public Movable(Game game, Mesh mesh, String identifier) {
        super(game,mesh, identifier);
    }

    public boolean isOkayToUpdatePosition(Vector newPos) {
        return game.isVectorInsideWorld(newPos);
    }

    public void moveForward(ModelTickInput params) {
        Vector[] rotationMatrix = getOrientation().getRotationMatrix().convertToColumnVectorArray();
        Vector z = rotationMatrix[2];
        Vector delta = z.scalarMul(movementSpeed * params.timeDelta * -1);
        Vector newPos = getPos().add(delta);

        if(isOkayToUpdatePosition(newPos)) {
            this.pos = newPos;
            translationDirection = translationDirection.add(delta);
        }
    }

    public void moveBackward(ModelTickInput params) {
        Vector[] rotationMatrix = getOrientation().getRotationMatrix().convertToColumnVectorArray();
        Vector z = rotationMatrix[2];
        Vector delta = z.scalarMul(movementSpeed * params.timeDelta);
        Vector newPos = getPos().add(delta);

        if(isOkayToUpdatePosition(newPos)) {
            this.pos = newPos;
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
