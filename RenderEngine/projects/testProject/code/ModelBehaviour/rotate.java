package ModelBehaviour;

import engine.Math.Quaternion;
import engine.Math.Vector;
import engine.model.ModelBehaviour;
import engine.model.Model;
import engine.model.ModelBehaviourTickInput;

public class rotate extends ModelBehaviour {
    @Override
    public void tick(Model m, ModelBehaviourTickInput params) {
        Quaternion rot = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}), 50 * params.timeDelta);
        Quaternion newQ = rot.multiply(m.getOrientation());
        m.setOrientation(newQ);
    }
}
