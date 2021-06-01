package Kurama.ComponentSystem.components.constraintGUI.RigidBodySystem;

import Kurama.ComponentSystem.components.constraintGUI.Boundary;
import Kurama.ComponentSystem.components.constraintGUI.BoundaryConfigurator;
import Kurama.ComponentSystem.components.constraintGUI.stretchSystem.StretchMessage;

public class RigidBodyConfigurator implements BoundaryConfigurator {

    @Override
    public Boundary configure(Boundary boundary) {

        boundary.addPreInteractionConstraint( (b, info, verificationData) -> !(info instanceof StretchMessage));
        boundary.interactor = new RigidBoundaryInteractor();
        boundary.IVRequestPackGenerator = new RigidBodyIVRG();
        return boundary;
    }
}
