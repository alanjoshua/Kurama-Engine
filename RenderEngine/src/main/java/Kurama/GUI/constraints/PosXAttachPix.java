package Kurama.GUI.constraints;

import Kurama.GUI.Component;

public class PosXAttachPix extends Constraint {

    public int posXOff;

    public PosXAttachPix(int posXOff) {
        this.posXOff = posXOff;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        float newX = parent.pos.get(0) + posXOff;
        current.pos.setDataElement(0, newX);
    }
}
