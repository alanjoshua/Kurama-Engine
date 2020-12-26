package Kurama.model;

import Kurama.scene.Scene;

public class ModelBehaviourTickInput {

    public float timeDelta;
    public Scene scene;

    public ModelBehaviourTickInput(float timeDelta, Scene scene) {
        this.timeDelta = timeDelta;
        this.scene = scene;
    }
}
