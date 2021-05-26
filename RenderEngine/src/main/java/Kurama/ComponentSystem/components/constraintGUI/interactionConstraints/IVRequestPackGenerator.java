package Kurama.ComponentSystem.components.constraintGUI.interactionConstraints;

import Kurama.ComponentSystem.components.constraintGUI.BoundMoveDataPack;

public interface IVRequestPackGenerator {

    public abstract BoundMoveDataPack getValidificationRequestPack(float deltaMove);

}
