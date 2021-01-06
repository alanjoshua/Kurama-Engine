package Kurama.GUI.constraints;

import Kurama.GUI.Component;

public class PosXYBottomRightAttachPix extends Constraint {

    public int posXOff;
    public int posYOff;

    public PosXYBottomRightAttachPix(int posXOff, int posYOff) {
        this.posXOff = posXOff;
        this.posYOff = posYOff;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        float newX = (parent.pos.get(0) + parent.width) - (posXOff + current.width);
        float newY = (parent.pos.get(1) + parent.height) - (posYOff + current.height);
        current.pos.setDataElement(0, newX);
        current.pos.setDataElement(1, newY);
    }

}
