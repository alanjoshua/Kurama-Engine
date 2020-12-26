package Kurama.audio;

import Kurama.Math.Quaternion;
import Kurama.Math.Vector;

import static org.lwjgl.openal.AL10.*;

public class SoundListener {

    public SoundListener() {
        this(new Vector(0, 0, 0));
    }

    public SoundListener(Vector position) {
        alListener3f(AL_POSITION, position.get(0), position.get(1), position.get(2));
        alListener3f(AL_VELOCITY, 0, 0, 0);
    }

    public void setSpeed(Vector speed) {
        alListener3f(AL_VELOCITY, speed.get(0), speed.get(1), speed.get(2));
    }

    public void setPosition(Vector position) {
        alListener3f(AL_POSITION, position.get(0), position.get(1), position.get(2));
    }

    public void setOrientation(Quaternion orientation) {
        var inverse = orientation.getInverse();
        Vector[] points = new Vector[2];
        points[0] = new Vector(0,0,1);
        points[1] = new Vector(0, 1, 0);
        var res = inverse.rotatePoints(points);
        setOrientation(res[0], res[1]);
    }

    public void setOrientation(Vector at, Vector up) {
        float[] data = new float[6];
        data[0] = at.get(0);
        data[1] = at.get(1);
        data[2] = at.get(2);
        data[3] = up.get(0);
        data[4] = up.get(1);
        data[5] = up.get(2);
        alListenerfv(AL_ORIENTATION, data);
    }

}
