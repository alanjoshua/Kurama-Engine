package Kurama.GUI.constraints;

import Kurama.GUI.components.Component;

public class WidthPix implements Constraint {

    public int width;

    public WidthPix(int width) {
        this.width = width;
    }


    @Override
    public void solveConstraint(Component parent, Component current) {
        current.width = width;
    }
}
