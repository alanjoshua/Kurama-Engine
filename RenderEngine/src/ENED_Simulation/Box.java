package ENED_Simulation;

import engine.Mesh.Mesh;
import engine.Math.Matrix;
import engine.Math.Vector;
import engine.game.Game;
import engine.model.Model;

public class Box extends Model {

    public Vector barCode; // 4 digit barcode
    public Integer zone;
    public float scanDirSensitivity = 0.95f;  //0 - not sensitive to direction, 1 - has to be exactly perpendicular
    public float scanRotationRange = 0.05f;
    public float scanXProximity = 2;

    public static int ZONE_A = 0;
    public static int ZONE_B = 1;
    public static int ZONE_C = 2;
    public static int ZONE_D = 3;


    public Box(Game game, Mesh mesh, String identifier, Vector barCode,Integer zone) {
        super(game,mesh, identifier);
        this.barCode = barCode;
        this.zone = zone;
    }

//    Will return true if the box is within scanning radius and robot is moving in correct direction(depends on box location in shelf).
//    Robot orientation does not matter as long as it is moving in the correct scan direction (eg, even if its moving backwards but in the correct scan direction, the box would be scanned)
//    Meaning, the robot has scanners in both sides

    public boolean isRobotInCorrectPositionToScan(Robot robot) {
//        Vector dir = robot.getPos().sub(this.getPos());
        Vector dir = robot.getPos().sub(this.getPos().add(robot.getDirectionToFrontFromCentre(this)));
        float dist = dir.getNorm();

        if(dist <= robot.scanRadius) {

            Matrix boxMatrix = this.getOrientation().getRotationMatrix();

            Vector cross = boxMatrix.getColumn(2).cross(robot.translationDirection);
            Vector temp = new Vector(new float[]{1,1,1});
            float travelDir = cross.dot(temp);

            Matrix robotInBoxView = this.getWorldToObject().matMul(robot.getPos().addDimensionToVec(1));
            float robotZ = robotInBoxView.getColumn(0).get(2);  // z position of robot from box's perspective

            float verticalDirection = boxMatrix.getColumn(2).dot(robot.getOrientation().getRotationMatrix().getColumn(0));

            if (travelDir >= scanDirSensitivity && robotZ >= 0 && verticalDirection > scanDirSensitivity) {
                return true;
            } else {
                return false;
            }

        }
        else {
            return false;
        }
    }

}
