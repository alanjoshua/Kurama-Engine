package Kurama.ComponentSystem.components.model.modelBehaviour;

import Kurama.ComponentSystem.components.model.Model;

public class UpdatePosVelAcc extends Behaviour {

    @Override
    public void tick(Model m, BehaviourTickInput params) {
        var timeDelta = params.timeDelta;
        m.velocity = m.velocity.add(m.acceleration.scalarMul(timeDelta));
        var detlaV = m.velocity.scalarMul(timeDelta);
        m.pos = m.pos.add(detlaV);
    }

}
