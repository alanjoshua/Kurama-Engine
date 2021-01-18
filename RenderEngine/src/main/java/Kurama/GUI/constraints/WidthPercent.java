package Kurama.GUI.constraints;

import Kurama.GUI.components.Component;

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
