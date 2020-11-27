package ModelBehaviour;

import engine.model.Model;
import engine.model.ModelBehaviour;
import engine.model.ModelBehaviourTickInput;

public class AttachToPlayer extends ModelBehaviour {

    @Override
    public void tick(Model m, ModelBehaviourTickInput params) {

        m.setPos(params.scene.camera.getPos().sub(params.scene.camera.getOrientation().
                getRotationMatrix().getColumn(2).removeDimensionFromVec(3)));

        m.setOrientation(params.scene.camera.getOrientation());

    }
}
