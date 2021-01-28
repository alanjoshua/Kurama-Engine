package Kurama.particle;

import Kurama.ComponentSystem.components.model.Model;

import java.util.ArrayList;
import java.util.List;

public abstract class ParticleGenerator {

    public List<Model> particles = new ArrayList<>();
    public Particle baseParticle = null;
    public String ID;
    public boolean isInsideFrustum = true;

    public ParticleGenerator(String id) {
        this.ID = id;
    }

    public abstract void cleanup();
    public abstract void tick(ParticleGeneratorTickInput params);
}
