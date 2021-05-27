package Kurama.ComponentSystem.components.constraintGUI.stretchSystem;

import Kurama.ComponentSystem.components.constraintGUI.Boundary;
import Kurama.ComponentSystem.components.constraintGUI.BoundaryConfigurator;

public class StretchSystemConfigurator implements BoundaryConfigurator {
    @Override
    public Boundary configure(Boundary boundary) {
        boundary.interactor = new StretchSystemInteractor();
        return boundary;
    }
}
