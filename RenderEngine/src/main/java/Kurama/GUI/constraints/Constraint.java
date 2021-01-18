package Kurama.GUI.constraints;

import Kurama.GUI.components.Component;

public interface Constraint {
    public abstract void solveConstraint(Component parent, Component current);
}
