package Kurama.ComponentSystem.components.constraintGUI.stretchSystem;

import Kurama.ComponentSystem.components.constraintGUI.Boundary;
import Kurama.ComponentSystem.components.constraintGUI.BoundaryConfigurator;
import Kurama.ComponentSystem.components.constraintGUI.interactionValidifiers.EnsureNoNearZeroDimensions;

public class StretchSystemConfigurator implements BoundaryConfigurator {
    @Override
    public Boundary configure(Boundary boundary) {
        boundary.IVRequestPackGenerator = new StretchIVRG();
        boundary.interactor = new StretchSystemInteractor();

        boundary.addPostInteractionValidifier(new EnsureNoNearZeroDimensions(0));
        return boundary;
    }

}
