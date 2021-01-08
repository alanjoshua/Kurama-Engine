package Kurama.GUI.constraints;

import Kurama.GUI.Component;

public class PosXYTopLeftAttachPercent implements Constraint {

    float posYPercent;
    float posXPercent;

    public PosXYTopLeftAttachPercent(float posXPercent, float posYPercent) {
        this.posXPercent = posXPercent;
        this.posYPercent = posYPercent;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        float newX = parent.pos.get(0) + parent.width*posXPercent;
        float newY = parent.pos.get(1) + parent.height*posYPercent;
        current.pos.setDataElement(0, newX);
        current.pos.setDataElement(1, newY);
    }
}
