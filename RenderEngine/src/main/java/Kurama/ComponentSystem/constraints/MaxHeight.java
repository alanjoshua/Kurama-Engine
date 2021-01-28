package Kurama.ComponentSystem.constraints;

import Kurama.ComponentSystem.components.Component;

public class MaxHeight implements Constraint {

    public int maxHeight;

    public MaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        if(current.height > maxHeight) {
            current.height = maxHeight;
        }
    }
}
