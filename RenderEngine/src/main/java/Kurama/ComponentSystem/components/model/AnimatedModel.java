package Kurama.ComponentSystem.components.model;

import Kurama.Math.Matrix;
import Kurama.Math.Quaternion;
import Kurama.Math.Transformation;
import Kurama.Mesh.Mesh;
import Kurama.game.Game;
import Kurama.geometry.MD5.Animation;
import Kurama.geometry.MD5.AnimationFrame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnimatedModel extends Model {

    public Map<String, Animation> animations;
    public Animation currentAnimation;

    public List<Matrix> currentJointTransformations;

    public AnimatedModel(Game game, List<Mesh> meshes, List<AnimationFrame> frames, float frameRate, String identifier) {
        super(game, meshes, identifier);

        for(var m: meshes) {
            m.isAnimatedSkeleton = true;
        }

        animations = new HashMap<>();
        animations.put("DEFAULT", new Animation("DEFAULT", frames, frameRate));
        currentAnimation = animations.get("DEFAULT");

        generateCurrentSkeleton(0);
    }

    public AnimatedModel(Game game, List<Mesh> meshes, Map<String, Animation> animations, Animation currentAnimation, String identifier) {
        super(game, meshes, identifier);

        for(var m: meshes) {
            m.isAnimatedSkeleton = true;
        }

        this.currentAnimation = currentAnimation;
        this.animations = animations;
        generateCurrentSkeleton(0);
    }

    public void generateCurrentSkeleton(float frameVal) {

        var animationFrames = currentAnimation.animationFrames;
        var numJoints = currentAnimation.numJoints;

        currentJointTransformations = new ArrayList<>(numJoints);

        var stringRep = String.valueOf(frameVal).split("\\.");
        var baseFrame = Integer.parseInt(stringRep[0]);
        var nextFrame = baseFrame + 1;
        var inter = Float.parseFloat("0."+stringRep[1]);

        if(nextFrame >= animationFrames.size()) {
            nextFrame = 0;
        }
       currentJointTransformations = slerpBetweenFrames(animationFrames.get(baseFrame), animationFrames.get(nextFrame),
               inter, numJoints);
    }

    public List<Matrix> slerpBetweenFrames(AnimationFrame frame1, AnimationFrame frame2,
                                           float inter, int numJoints) {

        List<Matrix> results = new ArrayList<>(numJoints);
        for(int i = 0;i < numJoints;i++) {
            var joint1 = frame1.joints.get(i);
            var joint2 = frame2.joints.get(i);

            var int_pos = joint1.pos.add(joint2.pos.sub(joint1.pos).scalarMul(inter));
            var int_orient = Quaternion.slerp(joint1.orient, joint2.orient, inter);
            var int_scale = joint1.scale.add(joint2.scale.sub(joint1.scale).scalarMul(inter));

            var trans = new Transformation(int_orient, int_pos, int_scale);
            var localMat = trans.getTransformationMatrix();
            results.add(localMat);
        }
        return results;
    }

    public void cycleFrame(float inc) {
        var animationFrames = currentAnimation.animationFrames;

        var newCount = currentAnimation.currentFrame + inc;
        if(newCount >= animationFrames.size()) {
            newCount = newCount % animationFrames.size();
        }

        if(newCount < 0) {
            var diff = -newCount;
            newCount = animationFrames.size() - (diff%animationFrames.size());
        }

        currentAnimation.currentFrame = newCount;

    }

}
