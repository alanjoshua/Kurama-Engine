package Kurama.GUI.constraints;

import Kurama.GUI.Component;

public class PosXYTopLeftAttachPix implements Constraint {

    public int posXOff;
    public int posYOff;

    public PosXYTopLeftAttachPix(int posXOff, int posYOff) {
        this.posXOff = posXOff;
        this.posYOff = posYOff;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        float newX = parent.pos.get(0) + posXOff;
        float newY = parent.pos.get(1) + posYOff;
        current.pos.setDataElement(0, newX);
        current.pos.setDataElement(1, newY);
    }
}
