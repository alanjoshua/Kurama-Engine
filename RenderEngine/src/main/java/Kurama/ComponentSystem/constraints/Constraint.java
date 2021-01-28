package Kurama.ComponentSystem.constraints;

import Kurama.ComponentSystem.components.Component;

public interface Constraint {
    public abstract void solveConstraint(Component parent, Component current);
}
