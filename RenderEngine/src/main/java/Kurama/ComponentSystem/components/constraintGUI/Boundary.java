package Kurama.ComponentSystem.components.constraintGUI;

import Kurama.ComponentSystem.automations.BoundaryInteractable;
import Kurama.ComponentSystem.automations.HeightPercent;
import Kurama.ComponentSystem.automations.WidthPercent;
import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.components.Rectangle;
import Kurama.ComponentSystem.components.constraintGUI.interactionConstraints.InteractionConstraint;
import Kurama.Math.Vector;
import Kurama.game.Game;

import java.util.ArrayList;
import java.util.List;

public class Boundary extends Rectangle {

    // in a H-Boundary, positive is top and negative is bottom.
    // in a V-boundary, positive is right and negative is left.

    public static enum BoundaryOrient {Vertical, Horizontal};
    public static IVRequestPackGenerator defaultIVRGenerator = (parent, boundary, dx, dy) -> new BoundInteractionMessage(null, dx, dy);;

    public BoundaryOrient boundaryOrient;

    public List<Boundary> negativeAttachments = new ArrayList<>();
    public List<Boundary> positiveAttachments = new ArrayList<>();
    public List<InteractionConstraint> interactionConstraints = new ArrayList<>();
    public Interactor interactor = new DefaultBoundaryInteractor();

    // Default behaviour - override it to change it
    public IVRequestPackGenerator IVRequestPackGenerator = defaultIVRGenerator;

    public boolean alreadyUpdated = false; // This would be reset in after interaction. Mainly used during border movement to prevent cycles

    public Boundary(Game game, Component parent, String identifier, BoundaryOrient orient) {
        super(game, parent, identifier);

        this.boundaryOrient = orient;
        this.color = new Vector(1,1,1,1);

        this.addOnClickDraggedAction(new BoundaryInteractable(this)); // This will set delta move, and call relevant methods to move the boundary

        if(boundaryOrient == BoundaryOrient.Horizontal) {
            this.height = 10;
            this.initAutomations.add(new WidthPercent(1));
        }
        else {
            this.width = 10;
            this.initAutomations.add(new HeightPercent(1));
        }
    }

    public Boundary(Game game, Component parent, String identifier, BoundaryOrient orient, BoundaryConfigurator configurator) {
        super(game, parent, identifier);

        this.boundaryOrient = orient;
        this.color = new Vector(1,1,1,1);

        this.addOnClickDraggedAction(new BoundaryInteractable(this)); // This will set delta move, and call relevant methods to move the boundary

        if(boundaryOrient == BoundaryOrient.Horizontal) {
            this.height = 10;
            this.initAutomations.add(new WidthPercent(1));
        }
        else {
            this.width = 10;
            this.initAutomations.add(new HeightPercent(1));
        }

        if(configurator != null) {
            configurator.configure(this);
        }
    }

    public Boundary addInteractionConstraint(InteractionConstraint i) {
        interactionConstraints.add(i);
        return this;
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

    public Boundary addConnectedBoundary(Boundary connection, int connectionType, int reverseConnectionType) {

        if(connectionType == 0) {
            negativeAttachments.add(connection);
        }
        else {
            positiveAttachments.add(connection);
        }
        connection.addConnectedBoundary(this, reverseConnectionType);
        return this;
    }

    public boolean isValidInteraction(BoundInteractionMessage info) {

        for(var i: interactionConstraints) {
            if(!i.isValid(this, info)) {
                return false;
            }
        }
        return true;
    }

    // Is intended to be overridden
    public void initialiseInteraction(float deltaMoveX, float deltaMoveY) {
        var data = IVRequestPackGenerator.getValidificationRequestPack(null,this, deltaMoveX, deltaMoveY);
        interact(data, null, -1);
        resetParams();
        System.out.println();
    }

    // Reset alreadyUpdated param
    public void resetParams() {
        this.alreadyUpdated = false;

        negativeAttachments.forEach(b -> {
            if(b.alreadyUpdated)
                b.resetParams();
        });
        positiveAttachments.forEach(b -> {
            if(b.alreadyUpdated)
                b.resetParams();
        });
    }

    public boolean interact(BoundInteractionMessage info, Boundary parent, int relativePos) {
        return interactor.interact(info, this, parent, relativePos);
    }

}
