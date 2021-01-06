package Kurama.GUI.constraints;

import Kurama.GUI.Component;

public class MaxWidth extends Constraint {

    public int maxWidth;

    public MaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        if(current.width > maxWidth) {
            current.width = maxWidth;
        }
    }
}
