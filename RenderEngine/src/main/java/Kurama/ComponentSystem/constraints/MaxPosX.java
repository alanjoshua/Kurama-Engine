package Kurama.ComponentSystem.constraints;

import Kurama.ComponentSystem.components.Component;

public class MaxPosX implements Constraint {

    int maxPos;

    public MaxPosX(int maxPos) {
        this.maxPos = maxPos;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        if(current.pos.get(0) + current.width/2f > maxPos) {
            current.pos.setDataElement(0, maxPos - current.width/2f);
        }
    }
}
