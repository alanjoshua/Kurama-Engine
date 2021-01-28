package Kurama.ComponentSystem.components.model.modelBehaviour;

import Kurama.camera.Camera;
import Kurama.ComponentSystem.components.model.Model;

public class AttachToCamera extends Behaviour {

    public Camera attachedCamera;

    public AttachToCamera(Camera camera) {
        attachedCamera = camera;
    }

    @Override
    public void tick(Model m, BehaviourTickInput params) {

        m.setPos(attachedCamera.getPos().sub(attachedCamera.getOrientation().
                getRotationMatrix().getColumn(2).removeDimensionFromVec(3)));

        m.setOrientation(attachedCamera.getOrientation());

    }
}
