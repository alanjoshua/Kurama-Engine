package Kurama.GUI.constraints;

import Kurama.GUI.Component;

public class PosXRightAttachPix extends Constraint {

    public int posXOff;

    public PosXRightAttachPix(int posXOff) {
        this.posXOff = posXOff;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        float newX = (parent.pos.get(0) + parent.width) - (posXOff + current.width);
        current.pos.setDataElement(0, newX);
    }

}
