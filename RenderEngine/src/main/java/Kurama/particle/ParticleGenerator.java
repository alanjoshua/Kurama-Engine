package Kurama.particle;

import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.components.model.Model;
import Kurama.ComponentSystem.components.model.SceneComponent;
import Kurama.game.Game;

import java.util.ArrayList;
import java.util.List;

public abstract class ParticleGenerator extends SceneComponent {

    public List<Model> particles = new ArrayList<>();
    public Particle baseParticle = null;
    public boolean isInsideFrustum = true;

    public ParticleGenerator(Game game, Component parent, String id) {
        super(game, parent, id);

        // automation to update particles
        automationsAfterPosConfirm.add((comp, input, timeDelta) ->
                this.particles.forEach(part -> part.tick(globalChildrenConstraints, input, timeDelta)));
    }

    public abstract void cleanup();

}
