package Kurama.ComponentSystem.constraints;

import Kurama.ComponentSystem.components.Component;

public class MinPosY implements Constraint {

    int minPos;

    public MinPosY(int minPos) {
        this.minPos = minPos;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        if(current.pos.get(1) - current.height/2f < minPos) {
            current.pos.setDataElement(1, minPos + current.height/2f);
        }
    }
}
