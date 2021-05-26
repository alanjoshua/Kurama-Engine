package Kurama.ComponentSystem.components.constraintGUI.interactionConstraints;

import Kurama.ComponentSystem.components.constraintGUI.BoundMoveDataPack;

public class TemporaryIVRG implements IVRequestPackGenerator {
    @Override
    public BoundMoveDataPack getValidificationRequestPack(float deltaMove) {
        return new TemporaryBoundMoveDataPack(deltaMove, -1);
    }
}
