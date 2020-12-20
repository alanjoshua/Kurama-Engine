package engine.particle;

import java.util.ArrayList;
import java.util.List;

public abstract class ParticleGenerator {

    public List<Particle> particles = new ArrayList<>();
    public Particle baseParticle = null;
    public String ID;

    public ParticleGenerator(String id) {
        this.ID = id;
    }

    public abstract void cleanup();
    public abstract void tick(ParticleGeneratorTickInput params);
}
