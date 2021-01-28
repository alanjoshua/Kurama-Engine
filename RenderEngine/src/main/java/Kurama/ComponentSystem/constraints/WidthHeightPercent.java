package Kurama.ComponentSystem.constraints;

import Kurama.ComponentSystem.components.Component;

public class WidthHeightPercent implements Constraint {

    public float widthPercent;
    public float heightPercent;

    public WidthHeightPercent(float widthPercent, float heightPercent) {
        this.widthPercent = widthPercent;
        this.heightPercent = heightPercent;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        current.width = (int)(parent.width * widthPercent);
        current.height = (int)(parent.height * heightPercent);
    }
}
