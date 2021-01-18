package Kurama.GUI.constraints;

import Kurama.GUI.Component;

public class PosYTopAttachPix implements Constraint {

    public int posYOff;

    public PosYTopAttachPix(int posYOff) {
        this.posYOff = posYOff;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        float newY = -parent.height/2f + posYOff + current.height / 2f;
        current.pos.setDataElement(1, newY);
    }
}
