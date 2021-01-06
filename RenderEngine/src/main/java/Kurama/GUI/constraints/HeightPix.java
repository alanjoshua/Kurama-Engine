package Kurama.GUI.constraints;

import Kurama.GUI.Component;

public class HeightPix extends Constraint {

    public int height;

    public HeightPix(int height) {
        this.height = height;
    }


    @Override
    public void solveConstraint(Component parent, Component current) {
        current.height = height;
    }
}
