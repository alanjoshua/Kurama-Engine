package Kurama.model;

import Kurama.Mesh.Mesh;
import Kurama.Math.Matrix;
import Kurama.Math.Quaternion;
import Kurama.Math.Vector;
import Kurama.game.Game;
import Kurama.model.modelBehaviour.BehaviourTickInput;

public class Movable extends Model {

    public Vector translationDirection;
    public Vector attemptedTranslation;
    public float rotationSpeed = 150;
    public float movementSpeed = 10;
    public boolean isManualControl = false;

    public Movable(Game game, Mesh mesh, String identifier) {
        super(game,mesh, identifier);
        translationDirection = new Vector(3,0);
        attemptedTranslation = new Vector(3,0);
    }

    public boolean isOkayToUpdatePosition(Vector newPos) {
        return game.isVectorInsideWorld(newPos);
    }

    public boolean isOkayToUpdateRotation(Matrix rot) {
        return true;
    }

    public boolean moveForward(BehaviourTickInput params) {
        Vector[] rotationMatrix = getOrientation().getRotationMatrix().convertToColumnVectorArray();
        Vector z = rotationMatrix[2];
        Vector delta = z.scalarMul(movementSpeed * params.timeDelta * -1);
        Vector newPos = getPos().add(delta);
        attemptedTranslation = attemptedTranslation.add(delta);

        if(isOkayToUpdatePosition(newPos)) {
            setPos(newPos);
            return true;
        }
        return false;
    }

    public boolean moveBackward(BehaviourTickInput params) {
        Vector[] rotationMatrix = getOrientation().getRotationMatrix().convertToColumnVectorArray();
        Vector z = rotationMatrix[2];
        Vector delta = z.scalarMul(movementSpeed * params.timeDelta);
        Vector newPos = getPos().add(delta);
        attemptedTranslation = attemptedTranslation.add(delta);

        if(isOkayToUpdatePosition(newPos)) {
            setPos(newPos);
            return true;
        }
        return false;
    }

    public boolean turnLeft(BehaviourTickInput params) {
        Quaternion rot = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}), rotationSpeed* params.timeDelta);
        if(isOkayToUpdateRotation(rot.getRotationMatrix())) {
            Quaternion newQ = rot.multiply(getOrientation());
            setOrientation(newQ);
            return true;
        }
        return false;
    }

    public boolean turnRight(BehaviourTickInput params) {
        Quaternion rot = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}), -rotationSpeed* params.timeDelta);
        if(isOkayToUpdateRotation(rot.getRotationMatrix())) {
            Quaternion newQ = rot.multiply(getOrientation());
            setOrientation(newQ);
            return true;
        }
        return false;
    }

    public boolean move(BehaviourTickInput params, float dist) {
        Vector[] rotationMatrix = getOrientation().getRotationMatrix().convertToColumnVectorArray();
        Vector z = rotationMatrix[2];
        Vector delta = z.scalarMul(dist * params.timeDelta);
        Vector newPos = getPos().add(delta);
        attemptedTranslation = attemptedTranslation.add(delta);

        if(isOkayToUpdatePosition(newPos)) {
            setPos(newPos);
            return true;
        }
        return false;
    }

    public boolean turn(BehaviourTickInput params, float angle) {
        Quaternion rot = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}), angle * params.timeDelta);
        if(isOkayToUpdateRotation(rot.getRotationMatrix())) {
            Quaternion newQ = rot.multiply(getOrientation());
            setOrientation(newQ);
            return true;
        }
        return false;
    }

}
