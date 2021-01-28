package Kurama.ComponentSystem.constraints;

import Kurama.ComponentSystem.components.Component;

public class PosYBottomAttachPix implements Constraint {

    public int posYOff;

    public PosYBottomAttachPix(int posYOff) {
        this.posYOff = posYOff;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        float newY = parent.height/2f - (posYOff + current.height / 2f);
        current.pos.setDataElement(1, newY);
    }

}