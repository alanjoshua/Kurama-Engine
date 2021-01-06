package Kurama.GUI.constraints;

import Kurama.GUI.Component;

public abstract class Constraint {
    public abstract void solveConstraint(Component parent, Component current);
}
