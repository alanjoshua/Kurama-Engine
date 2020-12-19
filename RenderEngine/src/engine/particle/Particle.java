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

    public Particle(Game game, Mesh mesh, Vector velocity, Vector acceleration, String identifier) {
        super(game, mesh, identifier);
        this.velocity = velocity;
        this.acceleration = acceleration;
    }

    public Particle(Game game, List<Mesh> meshes, Vector velocity, Vector acceleration, String identifier) {
        super(game, meshes, identifier);
        this.velocity = velocity;
        this.acceleration = acceleration;
    }

    public Particle(Particle p) {
        super(p.game, p.meshes, Utils.getUniqueID());
        this.pos = new Vector(p.pos);
        this.orientation = new Quaternion(p.orientation);
        this.velocity = new Vector(p.velocity);
        this.acceleration = new Vector(p.acceleration);
        this.timeToLive = p.timeToLive;
    }

    public float updateTimeToLive(float elapsedTime) {
        this.timeToLive -= elapsedTime;
        return this.timeToLive;
    }

    public void tick(float timeDelta) {
        velocity = velocity.add(acceleration.scalarMul(timeDelta));
        pos = pos.add(velocity.scalarMul(timeDelta));
    }

}
