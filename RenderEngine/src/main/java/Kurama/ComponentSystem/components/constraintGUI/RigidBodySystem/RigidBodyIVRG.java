package Kurama.ComponentSystem.components.constraintGUI.RigidBodySystem;

import Kurama.ComponentSystem.components.constraintGUI.BoundMoveDataPack;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;
import Kurama.ComponentSystem.components.constraintGUI.IVRequestPackGenerator;

public class RigidBodyIVRG implements IVRequestPackGenerator {
    @Override
    public BoundMoveDataPack getValidificationRequestPack(Boundary boundary, float deltaMoveX, float deltaMoveY) {

        if(boundary.boundaryOrient == Boundary.BoundaryOrient.Vertical) {
            return new BoundMoveDataPack(deltaMoveX, 0, -1);
        }
        else {
            return new BoundMoveDataPack(0, deltaMoveY, -1);
        }
    }
}
