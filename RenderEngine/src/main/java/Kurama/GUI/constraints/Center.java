package Kurama.GUI.constraints;

import Kurama.GUI.components.Component;
import Kurama.Math.Vector;

public class Center implements Constraint {
    @Override
    public void solveConstraint(Component parent, Component current) {
        current.pos = new Vector(3, 0);
    }
}
