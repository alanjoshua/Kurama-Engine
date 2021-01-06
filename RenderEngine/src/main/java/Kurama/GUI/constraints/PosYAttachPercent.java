package Kurama.GUI.constraints;

import Kurama.GUI.Component;

public class PosYAttachPercent extends Constraint {

    float posYPercent;

    public PosYAttachPercent(float posYPercent) {
        this.posYPercent = posYPercent;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        float newY = parent.pos.get(1) + parent.height*posYPercent;
        current.pos.setDataElement(1, newY);
    }
}
