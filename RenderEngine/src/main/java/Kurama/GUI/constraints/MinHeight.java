package Kurama.GUI.constraints;

import Kurama.GUI.components.Component;

public class MinHeight implements Constraint {

    public int minHeight;

    public MinHeight(int minHeight) {
        this.minHeight = minHeight;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        if(current.height < minHeight) {
            current.width = minHeight;
        }
    }
}
