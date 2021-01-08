package Kurama.GUI;

import Kurama.GUI.automations.Automation;
import Kurama.GUI.constraints.Constraint;
import Kurama.Math.Matrix;
import Kurama.Math.Quaternion;
import Kurama.Math.Vector;
import Kurama.Mesh.Texture;

import java.util.ArrayList;
import java.util.List;

public abstract class Component {

    public Vector pos = new Vector(new float[]{0,0,0});
    public Quaternion orientation = Quaternion.getAxisAsQuat(1,0,0,0);
    public int width;
    public int height;

    public Vector color = new Vector(0,0,0,1);
    public Texture texture = null;

    public String identifier;
    public boolean isContainerVisible = true;
    public boolean shouldRenderGroup = true;

    public Component parent = null;
    public List<Component> children = new ArrayList<>();
    public List<Constraint> constraints = new ArrayList<>();
    public List<Constraint> globalChildrenConstraints = new ArrayList<>();
    public List<Automation> automations = new ArrayList<>();

    public Component(Component parent, String identifier) {
        this.identifier = identifier;
        this.parent = parent;
    }

    public Component addConstraint(Constraint constraint) {
        this.constraints.add(constraint);
        return this;
    }

    public Component addAutomation(Automation automation) {
        this.automations.add(automation);
        return this;
    }

    public Matrix getObjectToWorldMatrix() {

        Matrix rotationMatrix = this.orientation.getRotationMatrix();
        Matrix scalingMatrix = Matrix.getDiagonalMatrix(new Vector(width, height, 1));
        Matrix rotScalMatrix = rotationMatrix.matMul(scalingMatrix);

        Matrix transformationMatrix = rotScalMatrix.addColumn(pos.add(new Vector(width/2, height/2, 0)));
        transformationMatrix = transformationMatrix.addRow(new Vector(new float[]{0, 0, 0, 1}));

        return transformationMatrix;
    }

    public void resolveConstraints(List<Constraint> parentGlobalConstraints, float layerNum) {

        for(var automation: automations) {
            automation.runAutomation(this);
        }

        for(var constraint: constraints) {
            constraint.solveConstraint(parent, this);
        }

        for(var globalConstraints: parentGlobalConstraints) {
            globalConstraints.solveConstraint(parent, this);
        }

        pos.setDataElement(2, layerNum);

        for(var child: children) {
            child.resolveConstraints(globalChildrenConstraints, layerNum+0.5f);
        }

    }

    // This is usually called when the current container is the master container
    public void resolveConstraints() {

        for(var automation: automations) {
            automation.runAutomation(this);
        }

        for(var constraint: constraints) {
            constraint.solveConstraint(parent, this);
        }

        float startLayer = -1;
        pos.setDataElement(2, startLayer);

        for(var child: children) {
            child.resolveConstraints(globalChildrenConstraints, startLayer+0.5f);
        }

    }

    public Matrix getWorldToObject() {
        Matrix m_ = orientation.getInverse().getRotationMatrix();
        Vector pos_ = (m_.matMul(pos).toVector()).scalarMul(-1);
        Matrix res = m_.addColumn(pos_);
        res = res.addRow(new Vector(new float[]{0,0,0,1}));
        return res;
    }

}
