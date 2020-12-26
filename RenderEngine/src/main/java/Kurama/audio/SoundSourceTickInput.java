package Kurama.audio;

import Kurama.camera.Camera;

public class SoundSourceTickInput {

    public float timeDelta;
    public Camera camera;

    public SoundSourceTickInput(float delta, Camera camera) {
        timeDelta = delta;
        this.camera = camera;
    }

}
