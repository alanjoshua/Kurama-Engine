package Kurama.geometry.MD5;

import java.util.List;

public class Animation {

    public List<AnimationFrame> animationFrames;
    public float frameRate;
    public int numJoints;
    public String name;
    public float currentFrame;

    public Animation(String name, List<AnimationFrame> animationFrames, float frameRate) {
        this.animationFrames = animationFrames;
        this.frameRate = frameRate;
        numJoints = animationFrames.get(0).joints.size();
        this.name = name;
    }

    public Animation(String name, List<AnimationFrame> animationFrames, int numJoints, float frameRate) {
        this.animationFrames = animationFrames;
        this.frameRate = frameRate;
        this.numJoints = numJoints;
        this.name = name;
    }

}
