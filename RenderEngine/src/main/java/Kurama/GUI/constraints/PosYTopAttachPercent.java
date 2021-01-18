package Kurama.GUI.constraints;

import Kurama.GUI.components.Component;

public class PosYTopAttachPercent implements Constraint {

    float posYPercent;

    public PosYTopAttachPercent(float posYPercent) {
        this.posYPercent = posYPercent;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        float newY = -parent.height/2f + parent.height*posYPercent + current.height / 2f;
        current.pos.setDataElement(1, newY);
    }
}
