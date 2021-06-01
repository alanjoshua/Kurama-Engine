package Kurama.ComponentSystem.components.constraintGUI;

import Kurama.ComponentSystem.automations.Automation;
import Kurama.ComponentSystem.automations.BoundaryInteractable;
import Kurama.ComponentSystem.automations.HeightPercent;
import Kurama.ComponentSystem.automations.WidthPercent;
import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.components.Rectangle;
import Kurama.ComponentSystem.components.constraintGUI.interactionConstraints.InteractionConstraint;
import Kurama.Math.Vector;
import Kurama.game.Game;
import Kurama.inputs.Input;

import java.util.ArrayList;
import java.util.List;

public class Boundary extends Rectangle {

    // in a H-Boundary, positive is top and negative is bottom.
    // in a V-boundary, positive is right and negative is left.

    public static enum BoundaryOrient {Vertical, Horizontal};
    public static IVRequestPackGenerator defaultIVRGenerator = (parent, boundary, dx, dy) -> new BoundInteractionMessage(null, dx, dy);

    public static Automation updateBoundaryAutomation = (Component current, Input input, float timeDelta) -> {
        var b = (Boundary)current;
        if(b.shouldUpdatePos) {
            b.pos = b.updatedPos;
        }
        if(b.shouldUpdateWidth) {
            b.width = (int) b.updatedWidth;
        }
        if(b.shouldUpdateHeight) {
            b.height = (int) b.updatedHeight;
        }
        b.shouldUpdatePos = false;
        b.shouldUpdateWidth = false;
        b.shouldUpdateHeight = false;
        b.alreadyVisited = false;
    };

    public BoundaryOrient boundaryOrient;

    public List<Boundary> negativeAttachments = new ArrayList<>();
    public List<Boundary> positiveAttachments = new ArrayList<>();
    public List<InteractionConstraint> preInteractionConstraints = new ArrayList<>();
    public List<InteractionConstraint> postInteractionConstraints = new ArrayList<>();
    public Interactor interactor = new DefaultBoundaryInteractor();

    // Default behaviour - override it to change it
    public IVRequestPackGenerator IVRequestPackGenerator = defaultIVRGenerator;

    public boolean alreadyVisited = false; // This would be reset in after interaction. Mainly used during border movement to prevent cycles

    public boolean shouldUpdatePos = false;
    public boolean shouldUpdateWidth = false;
    public boolean shouldUpdateHeight = false;
    public Vector updatedPos;
    public float updatedWidth;
    public float updatedHeight;

    public Boundary(Game game, Component parent, String identifier, BoundaryOrient orient) {
        this(game, parent, identifier, orient, null);
    }

    public Boundary(Game game, Component parent, String identifier, BoundaryOrient orient, BoundaryConfigurator configurator) {
        super(game, parent, identifier);

        this.boundaryOrient = orient;
        this.color = new Vector(1,1,1,1);

        this.addOnClickDraggedAction(new BoundaryInteractable(this)); // This will set delta move, and call relevant methods to move the boundary
        this.addAutomation(updateBoundaryAutomation);

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

    public Boundary addPreInteractionConstraint(InteractionConstraint i) {
        preInteractionConstraints.add(i);
        return this;
    }

    public Boundary addPostInteractionConstraint(InteractionConstraint i) {
        postInteractionConstraints.add(i);
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

    public boolean isValidInteraction_pre(BoundInteractionMessage info) {

        var vd = new ConstraintVerificationData(pos, width, height);

        for(var i: preInteractionConstraints) {
            if(!i.isValid(this, info, vd)) {
                return false;
            }
        }
        return true;
    }

    public boolean isValidInteraction_post(BoundInteractionMessage info) {

        var vd = new ConstraintVerificationData(pos, width, height);
        if(shouldUpdatePos) {
            vd.pos = updatedPos;
        }
        if(shouldUpdateWidth) {
            vd.width = (int) updatedWidth;
        }
        if(shouldUpdateHeight) {
            vd.height = (int) updatedHeight;
        }

        for(var i: preInteractionConstraints) {
            if(!i.isValid(this, info, vd)) {
                return false;
            }
        }
        return true;
    }

    // Is intended to be overridden
    public void initialiseInteraction(float deltaMoveX, float deltaMoveY) {
        var data = IVRequestPackGenerator.getValidificationRequestPack(null,this, deltaMoveX, deltaMoveY);
        interact(data, null, -1);
//        resetParams();
    }

    // Reset alreadyUpdated param
    public void resetParams() {
        this.alreadyVisited = false;

        negativeAttachments.forEach(b -> {
            if(b.alreadyVisited)
                b.resetParams();
        });
        positiveAttachments.forEach(b -> {
            if(b.alreadyVisited)
                b.resetParams();
        });
    }

    public boolean interact(BoundInteractionMessage info, Boundary parent, int relativePos) {
        var isValid = isValidInteraction_pre(info);
        if(!isValid) return false;

        isValid = interactor.interact(info, this, parent, relativePos);
        if(!isValid) return false;

        return isValidInteraction_post(info);
    }

}
