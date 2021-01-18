package Kurama.GUI.constraints;

import Kurama.GUI.Component;

public class PosXRightAttachPix implements Constraint {

    public int posXOff;

    public PosXRightAttachPix(int posXOff) {
        this.posXOff = posXOff;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        float newX = parent.width/2f - (posXOff + current.width / 2f);
        current.pos.setDataElement(0, newX);
    }

}
