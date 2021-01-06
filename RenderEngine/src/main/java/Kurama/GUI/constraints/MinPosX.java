package Kurama.GUI.constraints;

import Kurama.GUI.Component;

public class MinPosX extends Constraint {

    int minPos;

    public MinPosX(int minPos) {
        this.minPos = minPos;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        if(current.pos.get(0) < minPos) {
            current.pos.setDataElement(0, minPos);
        }
    }
}