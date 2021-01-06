package Kurama.GUI.constraints;

import Kurama.GUI.Component;

public class MaxPosX extends Constraint {

    int maxPos;

    public MaxPosX(int maxPos) {
        this.maxPos = maxPos;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        if(current.pos.get(0) + current.width > maxPos) {
            current.pos.setDataElement(0, maxPos - current.width);
        }
    }
}
