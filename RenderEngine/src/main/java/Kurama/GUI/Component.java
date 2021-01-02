package Kurama.gui;

import Kurama.Math.Matrix;
import Kurama.Math.Quaternion;
import Kurama.Math.Vector;

import java.util.ArrayList;
import java.util.List;

public abstract class Component {

    public Vector pos = new Vector(0,0,0);
    public Quaternion orientation = Quaternion.getAxisAsQuat(1,0,0,0);
    public Vector scale = new Vector(1,1,1);

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
        Matrix scalingMatrix = Matrix.getDiagonalMatrix(scale);
        Matrix rotScalMatrix = rotationMatrix.matMul(scalingMatrix);

        Matrix transformationMatrix = rotScalMatrix.addColumn(this.pos);
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
