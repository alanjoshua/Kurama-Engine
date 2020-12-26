package ModelBehaviour;

import Kurama.Math.Quaternion;
import Kurama.Math.Vector;
import Kurama.lighting.DirectionalLight;
import Kurama.model.Model;
import Kurama.model.ModelBehaviour;
import Kurama.model.ModelBehaviourTickInput;
import Kurama.scene.Scene;

public class SunRevolve extends ModelBehaviour {

    @Override
    public void tick(Model m, ModelBehaviourTickInput params) {

        Scene scene = params.scene;
        float timeDelta = params.timeDelta;

        DirectionalLight directionalLight = (DirectionalLight) m;
        m.setPos(m.getOrientation().getRotationMatrix().getColumn(2).scalarMul(-directionalLight.lightPosScale));

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
        double angRad = Math.toRadians(lightAngle);

        Quaternion rot = Quaternion.getAxisAsQuat(new Vector(new float[] {1,0,0}), delta);
        directionalLight.setOrientation(rot.multiply(directionalLight.getOrientation()));

        scene.skybox.meshes.get(0).materials.get(0).ambientColor = new Vector(4, directionalLight.intensity);

    }
}
