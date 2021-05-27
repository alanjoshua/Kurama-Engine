package Kurama.ComponentSystem.components.constraintGUI.RigidBodySystem;

import Kurama.ComponentSystem.components.constraintGUI.Boundary;
import Kurama.ComponentSystem.components.constraintGUI.BoundaryConfigurator;

public class RigidBodyConfigurator implements BoundaryConfigurator {

    @Override
    public Boundary configure(Boundary boundary) {
        boundary.addInteractionConstraint(new RecursiveValidifierConstraint());
        boundary.interactor = new RigidBoundaryInteractor();
        boundary.IVRequestPackGenerator = new RigidBodyIVRG();
        return boundary;
    }
}
