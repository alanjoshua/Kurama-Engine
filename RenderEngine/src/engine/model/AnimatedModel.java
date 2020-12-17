package engine.model;

import engine.Mesh.Mesh;
import engine.game.Game;
import engine.geometry.MD5.AnimationFrame;

import java.util.List;

public class AnimatedModel extends Model {

    public List<AnimationFrame> animationFrames;
    public int currentFrame = 0;
    public float frameRate = 24;

    public AnimatedModel(Game game, List<Mesh> meshes, List<AnimationFrame> frames, float frameRate, String identifier) {
        super(game, meshes, identifier);
        this.animationFrames = frames;
        this.frameRate = frameRate;
    }

    public void cycleFrame(int inc) {
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
