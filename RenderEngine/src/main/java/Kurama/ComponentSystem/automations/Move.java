package Kurama.ComponentSystem.automations;

import Kurama.ComponentSystem.components.Component;
import Kurama.Math.Vector;
import Kurama.inputs.Input;

public class Move implements Automation {

    public Vector moveDir;
    public Vector previousPos = null;

    public Move(Vector moveSpeed) {
        this.moveDir = moveSpeed;
    }

    @Override
    public void run(Component current, Input input, float timeDelta) {
        if(previousPos == null) {
            previousPos = current.getPos();
        }

        previousPos = previousPos.add(moveDir.scalarMul(timeDelta));
        current.setPos(previousPos.getCopy());

    }
}
