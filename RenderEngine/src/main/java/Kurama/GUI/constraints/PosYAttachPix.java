package Kurama.GUI.constraints;

import Kurama.GUI.Component;

public class PosYAttachPix extends Constraint {

    public int posYOff;

    public PosYAttachPix(int posYOff) {
        this.posYOff = posYOff;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        float newY = parent.pos.get(1) + posYOff;
        current.pos.setDataElement(1, newY);
    }
}
