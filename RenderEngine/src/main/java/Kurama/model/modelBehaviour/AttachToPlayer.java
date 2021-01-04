package Kurama.model.modelBehaviour;

import Kurama.camera.Camera;
import Kurama.model.Model;

public class AttachToPlayer extends ModelBehaviour {

    public Camera attachedCamera;

    public AttachToPlayer(Camera camera) {
        attachedCamera = camera;
    }

    @Override
    public void tick(Model m, ModelBehaviourTickInput params) {

        m.setPos(attachedCamera.getPos().sub(attachedCamera.getOrientation().
                getRotationMatrix().getColumn(2).removeDimensionFromVec(3)));

        m.setOrientation(attachedCamera.getOrientation());

    }
}
