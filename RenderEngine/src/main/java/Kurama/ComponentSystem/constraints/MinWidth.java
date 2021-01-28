package Kurama.ComponentSystem.constraints;

import Kurama.ComponentSystem.components.Component;

public class MinWidth implements Constraint {

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