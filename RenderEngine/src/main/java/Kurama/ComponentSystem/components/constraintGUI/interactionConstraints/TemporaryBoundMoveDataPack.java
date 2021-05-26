package Kurama.ComponentSystem.components.constraintGUI.interactionConstraints;

import Kurama.ComponentSystem.components.constraintGUI.BoundMoveDataPack;

public class TemporaryBoundMoveDataPack extends BoundMoveDataPack {

    // parentMoveDir: -1 = nothing, 0 = x, 1 = y

    public int parentMoveDir = -1;
    public TemporaryBoundMoveDataPack(float deltaMove, int parentMoveDir) {
        super(deltaMove);
        this.parentMoveDir = parentMoveDir;
    }
}
