package Kurama.ComponentSystem.constraints;

import Kurama.ComponentSystem.components.Component;

public class PosXYBottomRightAttachPercent implements Constraint {

    float posXPercent;
    float posYPercent;

    public PosXYBottomRightAttachPercent(float posXPercent, float posYPercent) {
        this.posXPercent = posXPercent;
        this.posYPercent = posYPercent;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        float newX = parent.width/2f - (parent.width*posXPercent + current.width / 2f);
        float newY = parent.height/2f - (parent.height*posYPercent + current.height / 2f);
        current.pos.setDataElement(0, newX);
        current.pos.setDataElement(1, newY);
    }
}
