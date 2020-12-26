package Kurama.geometry.MD5;

import java.util.ArrayList;
import java.util.List;

public class AnimationFrame {

//    public Matrix[] localJointMatrices;
//    public Matrix[] jointMatrices;  // Matrix multiplied with inv of bind joint.
    public List<Joint> joints;

    public AnimationFrame(int numJoints) {
//        localJointMatrices = new Matrix[numJoints];
//        jointMatrices = new Matrix[numJoints];
        joints = new ArrayList<>(numJoints);
    }

//    public void setMatrix(int index, Vector pos, Quaternion orient, Matrix inv) {
//        var localMat = orient.getRotationMatrix().addColumn(pos).addRow(new Vector(0, 0, 0,1));
//        localJointMatrices[index] = localMat;
//        jointMatrices[index] = localMat.matMul(inv);
////        jointMatrices[index] = Matrix.getDiagonalMatrix(new Vector(1,1,1,1));
//    }

}
