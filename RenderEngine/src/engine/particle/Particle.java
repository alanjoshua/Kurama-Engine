package engine.particle;

import engine.Math.Quaternion;
import engine.Math.Vector;
import engine.Mesh.Mesh;
import engine.game.Game;
import engine.model.Model;
import engine.utils.Utils;

import java.util.List;

public class Particle extends Model {

    public Vector velocity;
    public Vector acceleration;
    public float timeToLive;  // in seconds
    public float updateTexture;
    public float currentAnimationTime = 0;
    public int animationFrames;
    public int texPos = 0;

    public Particle(Game game, Mesh mesh, Vector velocity, Vector acceleration, float timeToLive, float updateTexture,
                    String identifier) {
        super(game, mesh, identifier);
        this.velocity = velocity;
        this.acceleration = acceleration;
        this.timeToLive = timeToLive;
        this.updateTexture = updateTexture;
        this.animationFrames = mesh.materials.get(0).texture.numRows * mesh.materials.get(0).texture.numCols;
    }

    public Particle(Game game, List<Mesh> meshes, Vector velocity, Vector acceleration, float timeToLive, float updateTexture,
                    String identifier) {
        super(game, meshes, identifier);
        this.velocity = velocity;
        this.acceleration = acceleration;
        this.timeToLive = timeToLive;
        this.updateTexture = updateTexture;
        this.animationFrames = meshes.get(0).materials.get(0).texture.numRows * meshes.get(0).materials.get(0).texture.numCols;
    }

    public Particle(Particle p) {
        super(p.game, p.meshes, Utils.getUniqueID());
        this.pos = new Vector(p.pos);
        this.orientation = new Quaternion(p.orientation);
        this.velocity = new Vector(p.velocity);
        this.acceleration = new Vector(p.acceleration);
        this.timeToLive = p.timeToLive;
        this.updateTexture = p.updateTexture;
        this.animationFrames = p.animationFrames;
    }

    public float updateTimeToLive(float elapsedTime) {
        this.timeToLive -= elapsedTime;
        this.currentAnimationTime += elapsedTime;

        if(currentAnimationTime >= updateTexture && this.animationFrames > 0) {
            this.currentAnimationTime = 0;
            int pos = this.texPos;
            pos++;
            if(pos < this.animationFrames) {
                this.texPos = pos;
            }
            else {
                this.texPos = 0;
            }
        }

        return this.timeToLive;
    }

    public void tick(float timeDelta) {
//        Logger.log("ticking");
        velocity = velocity.add(acceleration.scalarMul(timeDelta));
        var detlaV = velocity.scalarMul(timeDelta);
        pos = pos.add(detlaV);
//        Logger.log("delta V: "+detlaV.toString());
    }

}
