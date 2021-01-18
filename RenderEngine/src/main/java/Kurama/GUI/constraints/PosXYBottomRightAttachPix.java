package Kurama.GUI.constraints;

import Kurama.GUI.Component;

public class PosXYBottomRightAttachPix implements Constraint {

    public int posXOff;
    public int posYOff;

    public PosXYBottomRightAttachPix(int posXOff, int posYOff) {
        this.posXOff = posXOff;
        this.posYOff = posYOff;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        float newX = parent.width/2f - (posXOff + current.width / 2f);
        float newY = parent.height/2f - (posYOff + current.height / 2f);
        current.pos.setDataElement(0, newX);
        current.pos.setDataElement(1, newY);
    }

}
