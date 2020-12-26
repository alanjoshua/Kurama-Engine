package ModelBehaviour;

import Kurama.Math.Quaternion;
import Kurama.Math.Vector;
import Kurama.model.ModelBehaviour;
import Kurama.model.Model;
import Kurama.model.ModelBehaviourTickInput;

public class rotate extends ModelBehaviour {
    @Override
    public void tick(Model m, ModelBehaviourTickInput params) {
        Quaternion rot = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}), 50 * params.timeDelta);
        Quaternion newQ = rot.multiply(m.getOrientation());
        m.setOrientation(newQ);
    }
}
