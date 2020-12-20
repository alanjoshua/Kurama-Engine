package engine.model;

import engine.Math.Matrix;
import engine.Math.Quaternion;
import engine.Math.Vector;
import engine.Mesh.InstancedMesh;
import engine.Mesh.Mesh;
import engine.game.Game;
import engine.geometry.MD5.AnimationFrame;

import java.util.ArrayList;
import java.util.List;

public class AnimatedModel extends Model {

    public List<AnimationFrame> animationFrames;
    public float currentFrame = 0;
    public float frameRate = 24;
    public int numJoints;
    public List<Matrix> jointUnbindMatrices;
    public List<Matrix> currentJointTransformations;

    public AnimatedModel(Game game, List<Mesh> meshes, List<AnimationFrame> frames, List<Matrix> jointUnbindMatrices, float frameRate, String identifier) {
        super(game, meshes, identifier);

        // Temporary until support for animation instancing support is added
        for(var m: meshes) {
            if(m instanceof InstancedMesh) {
                try {
                    throw new Exception("Animated Models do not yet support instanced rendering");
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }

        this.animationFrames = frames;
        this.frameRate = frameRate;
        this.jointUnbindMatrices = jointUnbindMatrices;
        numJoints = animationFrames.get(0).joints.size();
        generateCurrentSkeleton(0);
    }

    public void generateCurrentSkeleton(float frameVal) {

        currentJointTransformations = new ArrayList<>(numJoints);

        var stringRep = String.valueOf(frameVal).split("\\.");
        var baseFrame = Integer.parseInt(stringRep[0]);
        var nextFrame = baseFrame + 1;
        var inter = Float.parseFloat("0."+stringRep[1]);

        if(nextFrame >= animationFrames.size()) {
            nextFrame = 0;
        }
       currentJointTransformations = slerpBetweenFrames(animationFrames.get(baseFrame), animationFrames.get(nextFrame), inter);
    }

    public List<Matrix> slerpBetweenFrames(AnimationFrame frame1, AnimationFrame frame2, float inter) {
        List<Matrix> results = new ArrayList<>(numJoints);
        for(int i = 0;i < numJoints;i++) {
            var joint1 = frame1.joints.get(i);
            var joint2 = frame2.joints.get(i);

            var int_pos = joint1.pos.add(joint2.pos.sub(joint1.pos).scalarMul(inter));
            var int_orient = Quaternion.slerp(joint1.orient, joint2.orient, inter);

            var localMat = int_orient.getRotationMatrix().addColumn(int_pos).addRow(new Vector(0,0,0,1));
            var jointMat = localMat.matMul(jointUnbindMatrices.get(i));
            results.add(jointMat);
        }
        return results;
    }

    public void cycleFrame(float inc) {
        var newCount = currentFrame + inc;
        if(newCount >= animationFrames.size()) {
            newCount = newCount % animationFrames.size();
        }

        if(newCount < 0) {
            var diff = -newCount;
            newCount = animationFrames.size() - (diff%animationFrames.size());
        }

        currentFrame = newCount;

    }

}
