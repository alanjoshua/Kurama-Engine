package Kurama.GUI.constraints;

import Kurama.GUI.components.Component;

public class MinPosX implements Constraint {

    int minPos;

    public MinPosX(int minPos) {
        this.minPos = minPos;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        if(current.pos.get(0) - current.width/2f < minPos) {
            current.pos.setDataElement(0, minPos + current.width/2f);
        }
    }
}