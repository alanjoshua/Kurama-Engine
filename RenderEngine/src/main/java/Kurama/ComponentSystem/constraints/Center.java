package Kurama.ComponentSystem.constraints;

import Kurama.ComponentSystem.components.Component;
import Kurama.Math.Vector;

public class Center implements Constraint {
    @Override
    public void solveConstraint(Component parent, Component current) {
        current.pos = new Vector(3, 0);
    }
}
