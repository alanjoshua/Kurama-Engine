package Kurama.GUI.constraints;

import Kurama.GUI.Component;

public class PosYBottomAttachPix extends Constraint {

    public int posYOff;

    public PosYBottomAttachPix(int posYOff) {
        this.posYOff = posYOff;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        float newY = (parent.pos.get(1) + parent.height) - (posYOff + current.height);
        current.pos.setDataElement(1, newY);
    }

}