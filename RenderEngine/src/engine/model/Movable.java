package engine.model;

import engine.DataStructure.Mesh.Mesh;
import engine.Math.Quaternion;
import engine.Math.Vector;
import engine.game.Game;

public class Movable extends Model {

    public Vector translationDirection;
    public float rotationSpeed = 150;
    public float movementSpeed = 10;
    public boolean isManualControl = false;

    public Movable(Game game, Mesh mesh, String identifier) {
        super(game,mesh, identifier);
    }

//    public boolean updatePosAfterBoundingBoxCheck(Vector newPos) {
//        if(boundMin!= null && boundMax!=null && newPos.get(0) >= boundMin.get(0) && newPos.get(0) < boundMax.get(0) && newPos.get(2) <= boundMin.get(1) && newPos.get(2) > boundMax.get(1)) {
//            this.pos = newPos;
//            return true;
//        }
//        return false;
//    }

    public void moveForward(ModelTickInput params) {
        Vector[] rotationMatrix = getOrientation().getRotationMatrix().convertToColumnVectorArray();

        Vector x = rotationMatrix[0];
        Vector y = new Vector(new float[] {0,1,0});
        Vector z = x.cross(y);
        Vector delta = z.scalarMul(movementSpeed * params.timeDelta * -1);
        Vector newPos = getPos().add(delta);

        if(game.isVectorInsideWorld(newPos)) {
            this.pos = newPos;
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

        if(game.isVectorInsideWorld(newPos)) {
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
