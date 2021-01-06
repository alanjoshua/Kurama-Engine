package Kurama.GUI.constraints;

import Kurama.GUI.Component;

public class MinPosY extends Constraint {

    int minPos;

    public MinPosY(int minPos) {
        this.minPos = minPos;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        if(current.pos.get(1) < minPos) {
            current.pos.setDataElement(1, minPos);
        }
    }
}
