package Kurama.ComponentSystem.components.model.modelBehaviour;

import Kurama.scene.Scene;

public class BehaviourTickInput {

    public float timeDelta;
    public Scene scene;

    public BehaviourTickInput(float timeDelta, Scene scene) {
        this.timeDelta = timeDelta;
        this.scene = scene;
    }
}
