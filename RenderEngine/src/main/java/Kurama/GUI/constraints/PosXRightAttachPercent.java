package Kurama.GUI.constraints;

import Kurama.GUI.Component;

public class PosXRightAttachPercent extends Constraint {

    float posXPercent;

    public PosXRightAttachPercent(float posXPercent) {
        this.posXPercent = posXPercent;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        float newX = (parent.pos.get(0) + parent.width ) - (posXPercent*parent.width + current.width);
        current.pos.setDataElement(0, newX);
    }
}
