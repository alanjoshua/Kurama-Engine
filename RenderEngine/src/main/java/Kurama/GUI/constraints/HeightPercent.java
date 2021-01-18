package Kurama.GUI.constraints;

import Kurama.GUI.components.Component;

public class HeightPercent implements Constraint {

    public float heightPercent;

    public HeightPercent(float heightPercent) {
        this.heightPercent = heightPercent;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        current.height = (int)(parent.height * heightPercent);
    }
}
