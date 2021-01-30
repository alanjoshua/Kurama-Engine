package ModelBehaviour;

import Kurama.Math.Quaternion;
import Kurama.Math.Vector;
import Kurama.lighting.DirectionalLight;
import Kurama.ComponentSystem.components.model.Model;
import Kurama.ComponentSystem.components.model.modelBehaviour.Behaviour;
import Kurama.ComponentSystem.components.model.modelBehaviour.BehaviourTickInput;
import Kurama.scene.Scene;

public class SunRevolve extends Behaviour {

    @Override
    public void tick(Model m, BehaviourTickInput params) {

        Scene scene = params.scene;
        float timeDelta = params.timeDelta;

        float lightPosScale = 500;

        DirectionalLight directionalLight = (DirectionalLight) m;
        m.setPos(m.getOrientation().getRotationMatrix().getColumn(2).scalarMul(-lightPosScale));

        float delta = (10f * timeDelta);
        float currentPitch = directionalLight.getOrientation().getPitchYawRoll().get(0);

        float lightAngle = currentPitch + delta;

        if (lightAngle > 180 || lightAngle < 0) {
            directionalLight.intensity = 0;

        } else if (lightAngle <= 10 || lightAngle >= 170) {
            float factor = (lightAngle > 10?180-lightAngle:lightAngle)/20f;
            directionalLight.intensity = factor;

        } else {
            directionalLight.intensity = 1;
            directionalLight.color = new Vector(3, 1);
        }

        Quaternion rot = Quaternion.getAxisAsQuat(new Vector(new float[] {1,0,0}), delta);
        directionalLight.setOrientation(rot.multiply(directionalLight.getOrientation()));

        scene.skybox.meshes.get(0).materials.get(0).ambientColor = new Vector(4, directionalLight.intensity);

    }
}
