package Kurama.ComponentSystem.components.constraintGUI;

import Kurama.ComponentSystem.automations.BoundaryMove;
import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.components.Rectangle;
import Kurama.game.Game;

import java.util.ArrayList;

public abstract class Boundary extends Rectangle {

    // in a H-Bound, positive is top and negative is bottom.
    // in a V-bound, positive is right and negative is left.

    public ArrayList<Boundary> negativeAttachments = new ArrayList<>();
    public ArrayList<Boundary> positiveAttachments = new ArrayList<>();

    public float deltaMove = 0;
    public boolean alreadyUpdated = false; // This would be reset in each tick. Mainly used during border movement to prevent cycles

    public Boundary(Game game, Component parent, String identifier) {
        super(game, parent, identifier);
        this.addAutomationAfterChildTick((c,i,t) -> {deltaMove = 0;alreadyUpdated = false;}); // Reset delta Move after children are ticked

        this.addOnClickDraggedAction(new BoundaryMove(this)); // This will set delta move, and call relevant methods to move the boundary
    }

    public boolean canBeMoved(BoundMoveDataPack info) {

        for(var b: negativeAttachments) {
            if(!b.canBeMoved(info)) {
                return false;
            }
        }
        for(var b: positiveAttachments) {
            if(!b.canBeMoved(info)) {
                return false;
            }
        }

        return true;
    }

    public Component addConnectedBoundary(Boundary connection, int connectionType) {
        if(connectionType == 0) {
            negativeAttachments.add(connection);
        }
        else {
            positiveAttachments.add(connection);
        }
        return this;
    }

    public Component addConnectedBoundary(Boundary connection, int connectionType, int reverseConnectionType) {

        if(connectionType == 0) {
            negativeAttachments.add(connection);
        }
        else {
            positiveAttachments.add(connection);
        }
        connection.addConnectedBoundary(this, reverseConnectionType);
        return this;
    }

    public void shouldMove() {
        var data = new BoundMoveDataPack(deltaMove);
        if(canBeMoved(data)) {
            move(data, null, -1);
        }
    }

    // Definitely moves everything

    // parentMoveDir: -1 = nothing, 0 = x, 1 = y
    public abstract void move(BoundMoveDataPack info, Boundary parent, int parentMoveDir);

}
