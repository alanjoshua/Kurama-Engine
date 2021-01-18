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

        float newX = -parent.width/2f + parent.width*posXPercent + current.width / 2f;
        float newY = -parent.height/2f + parent.height*posYPercent + current.height / 2f;

        current.pos.setDataElement(0, newX);
        current.pos.setDataElement(1, newY);
    }
}
