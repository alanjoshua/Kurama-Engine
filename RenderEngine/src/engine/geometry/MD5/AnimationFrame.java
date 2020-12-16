package engine.geometry.MD5;

import java.util.ArrayList;
import java.util.List;

public class AnimationFrame {

    List<Joint> joints;

    public AnimationFrame(int numJoints) {
        joints = new ArrayList<>(numJoints);
    }

}
