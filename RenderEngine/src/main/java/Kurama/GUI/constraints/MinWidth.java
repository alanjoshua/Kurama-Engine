package Kurama.GUI.constraints;

import Kurama.GUI.Component;

public class MinWidth extends Constraint {

    public int minWidth;

    public MinWidth(int minWidth) {
        this.minWidth = minWidth;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        if(current.width < minWidth) {
            current.width = minWidth;
        }
    }
}
