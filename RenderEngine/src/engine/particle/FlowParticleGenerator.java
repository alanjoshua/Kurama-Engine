package engine.particle;

import engine.Math.Vector;
import engine.utils.Logger;

public class FlowParticleGenerator extends ParticleGenerator {

    public int maxParticles;
    public boolean active;
    public long lastCreationTime;  //Nanoseconds
    public float posRange = 0;
    public float creationPeriodSeconds;
    public float velRange = 0;
    public float scaleRange = 0;

    public FlowParticleGenerator(Particle baseParticle, int maxParticles, float creationPeriodSeconds, String id) {
        super(id);
        this.baseParticle = baseParticle;
        this.maxParticles = maxParticles;
        this.creationPeriodSeconds = creationPeriodSeconds;
    }

    @Override
    public void cleanup() {
        for(Particle p: particles) {
            p.cleanUp();
        }
    }

    @Override
    public void tick(ParticleGeneratorTickInput params) {
        float timeDelta = params.timeDelta;  //Seconds

        long now = System.nanoTime();
        if(lastCreationTime == 0){
            lastCreationTime = now;
        }

        var it = particles.iterator();
        while(it.hasNext()) {
            var particle = it.next();
            if(particle.updateTimeToLive(timeDelta) < 0) {
                it.remove();
            }
            else {
                particle.tick(timeDelta);
            }
        }

        if((now - lastCreationTime)/1000000000.0 >= this.creationPeriodSeconds && particles.size() < maxParticles) {
            createParticle();
            this.lastCreationTime = now;
        }
    }

    public void createParticle() {
        Logger.log("Creating new particle");
        var particle = new Particle(this.baseParticle);
        float sign = Math.random() > 0.5d ? -1.0f : 1.0f;
        float speedInc = sign * (float)Math.random() * this.velRange;
        float posInc = sign * (float)Math.random() * this.posRange;
        float scaleInc = sign * (float)Math.random() * this.scaleRange;

        particle.pos = particle.pos.add(new Vector(posInc, posInc, posInc));
        particle.velocity = particle.velocity.add(new Vector(speedInc, speedInc, speedInc));
        particle.scale = particle.scale.add(new Vector(scaleInc, scaleInc, scaleInc));
        particles.add(particle);
    }

}
