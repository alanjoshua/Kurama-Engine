package Kurama.GUI.constraints;

import Kurama.GUI.Component;

public class WidthPix extends Constraint {

    public int width;

    public WidthPix(int width) {
        this.width = width;
    }


    @Override
    public void solveConstraint(Component parent, Component current) {
        current.width = width;
    }
}
