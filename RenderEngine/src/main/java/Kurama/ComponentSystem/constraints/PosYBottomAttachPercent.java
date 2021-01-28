package Kurama.ComponentSystem.constraints;

import Kurama.ComponentSystem.components.Component;

public class PosYBottomAttachPercent implements Constraint {

    float posYPercent;

    public PosYBottomAttachPercent(float posYPercent) {
        this.posYPercent = posYPercent;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        float newY = parent.height/2f - (parent.height*posYPercent + current.height / 2f);
        current.pos.setDataElement(1, newY);
    }
}
