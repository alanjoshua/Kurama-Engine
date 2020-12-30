package Kurama.geometry.MD5;

import Kurama.Math.Matrix;

import java.util.List;

public class Animation {

    public List<AnimationFrame> animationFrames;
    public List<Matrix> jointUnbindMatrices;
    public float frameRate;
    public int numJoints;
    public String name;
    public float currentFrame;

    public Animation(String name, List<AnimationFrame> animationFrames, List<Matrix> jointUnbindMatrices, float frameRate) {
        this.animationFrames = animationFrames;
        this.frameRate = frameRate;
        this.jointUnbindMatrices = jointUnbindMatrices;
        numJoints = animationFrames.get(0).joints.size();
        this.name = name;
    }

    public Animation(String name, List<AnimationFrame> animationFrames, int numJoints, List<Matrix> jointUnbindMatrices, float frameRate) {
        this.animationFrames = animationFrames;
        this.frameRate = frameRate;
        this.jointUnbindMatrices = jointUnbindMatrices;
        this.numJoints = numJoints;
        this.name = name;
    }

}
