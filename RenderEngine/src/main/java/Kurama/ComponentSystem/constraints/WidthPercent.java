package Kurama.ComponentSystem.constraints;

import Kurama.ComponentSystem.components.Component;

public class WidthPercent implements Constraint {

    public float widthPercent;

    public WidthPercent(float widthPercent) {
        this.widthPercent = widthPercent;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        current.width = (int)(parent.width * widthPercent);
    }
}
