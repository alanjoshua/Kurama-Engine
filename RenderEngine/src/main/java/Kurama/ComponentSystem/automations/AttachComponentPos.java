package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.constraints.Constraint;

public class AttachComponentPos implements Constraint {

    Component comp;
    public AttachComponentPos(Component comp) {
        this.comp = comp;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        current.pos = comp.pos;
    }
}
