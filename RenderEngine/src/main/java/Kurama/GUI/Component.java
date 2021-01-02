package Kurama.GUI;

import Kurama.Math.Matrix;
import Kurama.Math.Quaternion;
import Kurama.Math.Vector;

import java.util.ArrayList;
import java.util.List;

public abstract class Component {

    public Vector pos = new Vector(new float[]{0,0,0});
    public Quaternion orientation = Quaternion.getAxisAsQuat(1,0,0,0);
    public int width;
    public int height;

    public String identifier;
    public boolean isHidden = false;

    public Component parent = null;
    public List<Component> children = new ArrayList<>();

    public Component(Component parent, String identifier) {
        this.identifier = identifier;
        this.parent = parent;
    }

    public Matrix getObjectToWorldMatrix() {

        Matrix rotationMatrix = this.orientation.getRotationMatrix();
        Matrix scalingMatrix = Matrix.getDiagonalMatrix(new Vector(width, height, 1));
        Matrix rotScalMatrix = rotationMatrix.matMul(scalingMatrix);

        Matrix transformationMatrix = rotScalMatrix.addColumn(pos.add(new Vector(width/2, height/2, 0)));
        transformationMatrix = transformationMatrix.addRow(new Vector(new float[]{0, 0, 0, 1}));

        return transformationMatrix;
    }

    public Matrix getWorldToObject() {
        Matrix m_ = orientation.getInverse().getRotationMatrix();
        Vector pos_ = (m_.matMul(pos).toVector()).scalarMul(-1);
        Matrix res = m_.addColumn(pos_);
        res = res.addRow(new Vector(new float[]{0,0,0,1}));
        return res;
    }

}
