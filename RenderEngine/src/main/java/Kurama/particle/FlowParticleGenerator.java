package Kurama.particle;

import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.components.model.Model;
import Kurama.Math.Vector;
import Kurama.game.Game;

public class FlowParticleGenerator extends ParticleGenerator {

    public int maxParticles;
    public boolean active;
    public long lastCreationTime;  //Nanoseconds
    public Vector posRange = new Vector(0,0,0);
    public float creationPeriodSeconds;
    public Vector velRange = new Vector(0,0,0);
    public Vector accelRange = new Vector(0,0,0);
    public Vector scaleRange = new Vector(0,0,0);
    public float animUpdateRange = 0;

    public FlowParticleGenerator(Game game, Component parent, Particle baseParticle, int maxParticles, float creationPeriodSeconds, String id) {
        super(game, parent, id);

        this.baseParticle = baseParticle;
        this.baseParticle.parent = this;
        this.baseParticle.pos = new Vector(0,0,0);  // makes sure that the base particle is at the particle generator's position

        this.maxParticles = maxParticles;
        this.creationPeriodSeconds = creationPeriodSeconds;

        // update particles. This is run once every frame
        this.addAutomation((current, input, timeDelta) -> {
            long now = System.nanoTime();
            if(lastCreationTime == 0){
                lastCreationTime = now;
            }

            var it = particles.iterator();
            while(it.hasNext()) {
                var particle = (Particle)it.next();
                if(particle.updateTimeToLive(timeDelta) < 0) {
                    it.remove();
                    children.remove(particle);
                }
                else {
//                    particle.tick(timeDelta);d
                }
            }

            if((now - lastCreationTime)/1000000000.0 >= this.creationPeriodSeconds && children.size() < maxParticles) {
                createParticle();
                this.lastCreationTime = now;
            }
        });
    }

    @Override
    public void cleanup() {
        for(Model p: particles) {
            p.cleanUp();
        }
    }

    public void createParticle() {

        var particle = new Particle(this.baseParticle);
        float sign = Math.random() > 0.5d ? -1.0f : 1.0f;

        Vector speedInc = Vector.randomVector(3).mul(velRange);
        Vector posInc = Vector.randomVector(3).mul(posRange);
        Vector scaleInc = Vector.randomVector(3).mul(scaleRange);
        Vector accelInc = Vector.randomVector(3).mul(accelRange);
        float updateAnimInc = (long)sign *(long)(Math.random() * this.animUpdateRange);

        particle.acceleration = particle.acceleration.add(accelInc);
        particle.pos = particle.pos.add(posInc);
        particle.velocity = particle.velocity.add(speedInc);
        particle.scale = particle.scale.add(scaleInc);
        particle.updateTexture += updateAnimInc;

        particles.add(particle);
        addChild(particle);
    }

}
