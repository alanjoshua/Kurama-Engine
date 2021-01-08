package Kurama.GUI.constraints;

import Kurama.GUI.Component;

public class PosYBottomAttachPercent implements Constraint {

    float posYPercent;

    public PosYBottomAttachPercent(float posYPercent) {
        this.posYPercent = posYPercent;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        float newY = (parent.pos.get(1) + parent.height ) - (posYPercent*parent.height+ current.height);
        current.pos.setDataElement(1, newY);
    }
}
