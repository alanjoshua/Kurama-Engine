package Kurama.GUI.constraints;

import Kurama.GUI.Component;

public class MaxPosY implements Constraint {

    int maxPos;

    public MaxPosY(int maxPos) {
        this.maxPos = maxPos;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        if(current.pos.get(1) + current.height/2f> maxPos) {
            current.pos.setDataElement(1, maxPos - current.height/2f);
        }
    }
}