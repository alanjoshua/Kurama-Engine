package Kurama.Math;

import Kurama.geometry.MD5.Joint;

import java.util.ArrayList;
import java.util.List;

public class Transformation {

    public Quaternion orientation;
    public Vector pos;
    public Vector scale;

    public Transformation(Quaternion orientation, Vector pos) {
        this(orientation, pos, new Vector(1,1,1));
    }

    public Transformation(Quaternion orientation, Vector pos, Vector scale) {
        this.orientation = orientation;
        this.scale = scale;
        this.pos = pos;
    }

    public Transformation(Transformation t) {
        this.orientation = new Quaternion(t.orientation);
        this.pos = new Vector(t.pos);
        this.scale = new Vector(t.scale);
    }

    public Transformation(Matrix transformation) {
        this.pos = transformation.getColumn(3).removeDimensionFromVec(3);

        var rotScaleMatrix = transformation.getSubMatrix(0,0,2,2);

        List<Vector> rotCols = new ArrayList<>();
        var scale = new Vector(1,1,1);
        for(int i = 0;i < 3;i++) {
            var v = rotScaleMatrix.getColumn(i);
            scale.setDataElement(i, v.getNorm());
            v.normalise();
            rotCols.add(v);
        }

        var rotMatrix = new Matrix(rotCols);
        this.orientation = new Quaternion(rotMatrix);
        this.orientation.normalise();
        this.scale = scale;
    }

    public Transformation(Joint j) {
        this.pos = new Vector(j.pos);
        this.orientation = new Quaternion(j.orient);
        this.scale = new Vector(j.scale);
    }

    public Matrix getTransformationMatrix() {
        Matrix rotationMatrix = this.orientation.getRotationMatrix();
        Matrix scalingMatrix = Matrix.getDiagonalMatrix(scale);
        Matrix rotScalMatrix = rotationMatrix.matMul(scalingMatrix);

        Matrix transformationMatrix = rotScalMatrix.addColumn(this.pos);
        transformationMatrix = transformationMatrix.addRow(new Vector(new float[]{0, 0, 0, 1}));

        return transformationMatrix;
    }

    public Transformation matMul(Transformation t2) {
        Vector pos;
        Quaternion orient;

        var rotatedPoint = orientation.rotatePoint(t2.pos);
        pos = rotatedPoint.add(this.pos);
        orient = orientation.multiply(t2.orientation);
        orient.normalise();

        return new Transformation(orient, pos);
    }

}
