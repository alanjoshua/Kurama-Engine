package Kurama.ComponentSystem.components.constraintGUI.interactionConstraints;

import Kurama.ComponentSystem.components.constraintGUI.BoundMoveDataPack;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;

public interface Interaction {

    public abstract void interact(BoundMoveDataPack info, Boundary boundary, Boundary parentBoundary);

}
