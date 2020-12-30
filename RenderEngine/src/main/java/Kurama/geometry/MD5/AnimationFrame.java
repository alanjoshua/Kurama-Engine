package Kurama.geometry.MD5;

import java.util.ArrayList;
import java.util.List;

public class AnimationFrame {

    public List<Joint> joints;

    public AnimationFrame(int numJoints) {
        joints = new ArrayList<>(numJoints);
    }

    public AnimationFrame() {
        joints = new ArrayList<>();
    }
}
