package Kurama.GUI.constraints;

import Kurama.GUI.Component;

public interface Constraint {
    public abstract void solveConstraint(Component parent, Component current);
}
