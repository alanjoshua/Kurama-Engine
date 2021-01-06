package Kurama.model.modelBehaviour;

import Kurama.Math.Quaternion;
import Kurama.Math.Vector;
import Kurama.model.Model;

public class rotate extends Behaviour {
    @Override
    public void tick(Model m, BehaviourTickInput params) {
        Quaternion rot = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}), 50 * params.timeDelta);
        Quaternion newQ = rot.multiply(m.getOrientation());
        m.setOrientation(newQ);
    }
}
