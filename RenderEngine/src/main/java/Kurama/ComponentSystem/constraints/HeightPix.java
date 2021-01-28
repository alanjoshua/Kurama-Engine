package Kurama.ComponentSystem.constraints;

import Kurama.ComponentSystem.components.Component;

public class HeightPix implements Constraint {

    public int height;

    public HeightPix(int height) {
        this.height = height;
    }


    @Override
    public void solveConstraint(Component parent, Component current) {
        current.height = height;
    }
}
