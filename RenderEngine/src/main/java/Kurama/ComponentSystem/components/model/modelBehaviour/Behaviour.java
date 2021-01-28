package Kurama.ComponentSystem.components.model.modelBehaviour;

import Kurama.ComponentSystem.components.model.Model;

public abstract class Behaviour {

    public abstract void tick(Model m, BehaviourTickInput params);

}
