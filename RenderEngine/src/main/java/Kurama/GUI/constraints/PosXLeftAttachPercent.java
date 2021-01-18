package Kurama.GUI.constraints;

import Kurama.GUI.Component;

public class PosXLeftAttachPercent implements Constraint {

    float posXPercent;

    public PosXLeftAttachPercent(float posXPercent) {
        this.posXPercent = posXPercent;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        float newX = -parent.width/2f + parent.width*posXPercent + current.width / 2f;
        current.pos.setDataElement(0, newX);
    }
}
