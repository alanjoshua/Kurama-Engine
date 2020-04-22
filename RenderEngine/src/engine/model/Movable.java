package engine.model;

import engine.DataStructure.Mesh.Mesh;
import engine.Math.Matrix;
import engine.Math.Quaternion;
import engine.Math.Vector;
import engine.game.Game;

public class Movable extends Model {

    public enum MOVEMENT {
        FORWARD,BACKWARD,LEFT,RIGHT
    }

    public Vector translationDirection;
    public Vector rotationDirection;
    public Vector finalMovement;
    public float rotationSpeed = 150;
    public float movementSpeed = 10;
    public boolean isManualControl = false;

    public Movable(Game game, Mesh mesh, String identifier) {
        super(game,mesh, identifier);
        translationDirection = new Vector(3,0);
        rotationDirection = new Vector(3,0);
        finalMovement = new Vector(3,0);
    }

    public boolean isOkayToUpdatePosition(Vector newPos) {
        return game.isVectorInsideWorld(newPos);
    }

    public boolean isOkayToUpdateRotation(Matrix rot) {
        return true;
    }

    public boolean moveForward(ModelTickInput params) {
        Vector[] rotationMatrix = getOrientation().getRotationMatrix().convertToColumnVectorArray();
        Vector z = rotationMatrix[2];
        Vector delta = z.scalarMul(movementSpeed * params.timeDelta * -1);
        Vector newPos = getPos().add(delta);

        if(isOkayToUpdatePosition(newPos)) {
            this.pos = newPos;
            translationDirection = translationDirection.add(delta);
            return true;
        }
        return false;
    }

    public boolean moveBackward(ModelTickInput params) {
        Vector[] rotationMatrix = getOrientation().getRotationMatrix().convertToColumnVectorArray();
        Vector z = rotationMatrix[2];
        Vector delta = z.scalarMul(movementSpeed * params.timeDelta);
        Vector newPos = getPos().add(delta);

        if(isOkayToUpdatePosition(newPos)) {
            this.pos = newPos;
            translationDirection = translationDirection.add(delta);
            return true;
        }
        return false;
    }

    public boolean turnLeft(ModelTickInput params) {
        Quaternion rot = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}), rotationSpeed* params.timeDelta);
        if(isOkayToUpdateRotation(rot.getRotationMatrix())) {
            Vector temp = new Vector(new float[]{(float)Math.sin(Math.toRadians(rotationSpeed* params.timeDelta)),0,(float)Math.cos(Math.toRadians(rotationSpeed* params.timeDelta))});
            rotationDirection = rotationDirection.add(temp);
            Quaternion newQ = rot.multiply(getOrientation());
            setOrientation(newQ);
            return true;
        }
        return false;
    }

    public boolean turnRight(ModelTickInput params) {
        Quaternion rot = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}), -rotationSpeed* params.timeDelta);
        if(isOkayToUpdateRotation(rot.getRotationMatrix())) {
            Vector temp = new Vector(new float[]{(float)Math.sin(Math.toRadians(-rotationSpeed* params.timeDelta)),0,(float)Math.cos(Math.toRadians(-rotationSpeed* params.timeDelta))});
            rotationDirection = rotationDirection.add(temp);
            Quaternion newQ = rot.multiply(getOrientation());
            setOrientation(newQ);
            return true;
        }
        return false;
    }

    public boolean move(ModelTickInput params,float dist) {
        Vector[] rotationMatrix = getOrientation().getRotationMatrix().convertToColumnVectorArray();
        Vector z = rotationMatrix[2];
        Vector delta = z.scalarMul(dist * params.timeDelta);
        Vector newPos = getPos().add(delta);

        if(isOkayToUpdatePosition(newPos)) {
            this.pos = newPos;
            translationDirection = translationDirection.add(delta);
            return true;
        }
        return false;
    }

    public boolean turn(ModelTickInput params, float angle) {
        Quaternion rot = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}), angle * params.timeDelta);
        if(isOkayToUpdateRotation(rot.getRotationMatrix())) {
            Vector temp = new Vector(new float[]{(float)Math.sin(angle),0,(float)Math.cos(angle)});
            rotationDirection = rotationDirection.add(temp);
            Quaternion newQ = rot.multiply(getOrientation());
            setOrientation(newQ);
            return true;
        }
        return false;
    }

}
