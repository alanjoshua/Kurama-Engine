package Kurama.ComponentSystem.constraints;

import Kurama.ComponentSystem.components.Component;

public class MaxWidth implements Constraint {

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
