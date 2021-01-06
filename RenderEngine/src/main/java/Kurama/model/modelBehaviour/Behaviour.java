package Kurama.model.modelBehaviour;

import Kurama.model.Model;

public abstract class Behaviour {

    public abstract void tick(Model m, BehaviourTickInput params);

}
