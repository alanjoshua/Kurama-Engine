package Kurama.GUI.constraints;

import Kurama.GUI.Component;

public class PosYTopAttachPix implements Constraint {

    public int posYOff;

    public PosYTopAttachPix(int posYOff) {
        this.posYOff = posYOff;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        float newY = parent.pos.get(1) + posYOff;
        current.pos.setDataElement(1, newY);
    }
}
