package Kurama.GUI.constraints;

import Kurama.GUI.Component;

public class PosXYBottomRightAttachPercent extends Constraint {

    float posXPercent;
    float posYPercent;

    public PosXYBottomRightAttachPercent(float posXPercent, float posYPercent) {
        this.posXPercent = posXPercent;
        this.posYPercent = posYPercent;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        float newX = (parent.pos.get(0) + parent.width ) - (posXPercent*parent.width + current.width);
        float newY = (parent.pos.get(1) + parent.height ) - (posYPercent*parent.height+ current.height);
        current.pos.setDataElement(0, newX);
        current.pos.setDataElement(1, newY);
    }
}
