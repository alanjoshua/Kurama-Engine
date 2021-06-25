package Kurama.ComponentSystem.components.constraintGUI.stretchSystem;

import Kurama.ComponentSystem.components.constraintGUI.Boundary;
import Kurama.ComponentSystem.components.constraintGUI.BoundaryConfigurator;

public class StretchSystemConfigurator implements BoundaryConfigurator {
    @Override
    public Boundary configure(Boundary boundary) {
        boundary.IVRequestPackGenerator = new StretchIVRG();
        boundary.interactor = new StretchSystemInteractor();

        if(boundary.boundaryOrient == Boundary.BoundaryOrient.Vertical) {
            boundary.minHeight = 30;
        }
        else {
            boundary.minWidth = 50;
        }

        boundary.addPostInteractionValidifier(new EnsureDimensionsValid());
        boundary.addPostInteractionValidifier(new EnsureWithinWindow());

        return boundary;
    }

}
