package Kurama.particle;

import Kurama.ComponentSystem.components.Component;
import Kurama.Math.Quaternion;
import Kurama.Math.Vector;
import Kurama.Mesh.Mesh;
import Kurama.game.Game;
import Kurama.ComponentSystem.components.model.Model;
import Kurama.utils.Logger;
import Kurama.utils.Utils;

public class Particle extends Model {

    public float timeToLive;  // in seconds
    public float updateTexture;
    public float currentAnimationTime = 0;
    public int animationFrames;

    public Particle(Game game, Component parent, Mesh mesh, Vector velocity, Vector acceleration, float timeToLive, float updateTexture,
                    String identifier) {
        super(game, mesh, identifier);

        if(mesh.materials.size() > 1) {
            Logger.logError("A Particle mesh should only have one material. This mesh:"+mesh.meshIdentifier +
                    " has multiple materials. The particle shader will ignore the other materials.");
        }

        this.parent = parent;
        this.velocity = velocity;
        this.acceleration = acceleration;
        this.timeToLive = timeToLive;
        this.updateTexture = updateTexture;
        this.animationFrames = mesh.materials.get(0).texture.numRows * mesh.materials.get(0).texture.numCols;
    }

    public Particle(Particle p) {
        super(p.game, p.meshes, Utils.getUniqueID());
        this.pos = new Vector(p.pos);
        this.parent = p.parent;
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
            var mesh1 = meshes.get(0);
            int pos = matAtlasOffset.get(mesh1.meshIdentifier).get(0);
            pos++;
            if(pos < this.animationFrames) {
                matAtlasOffset.get(mesh1.meshIdentifier).set(0, pos);
            }
            else {
                matAtlasOffset.get(mesh1.meshIdentifier).set(0, 0);
            }
        }

        return this.timeToLive;
    }

//    public void tick(float timeDelta) {
//        Logger.log("ticking");
//        velocity = velocity.add(acceleration.scalarMul(timeDelta));
//        var detlaV = velocity.scalarMul(timeDelta);
//        pos = pos.add(detlaV);
//        Logger.log("delta V: "+detlaV.toString());
//    }

}
