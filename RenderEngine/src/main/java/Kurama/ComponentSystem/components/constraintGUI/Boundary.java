package Kurama.ComponentSystem.components.constraintGUI;

import Kurama.ComponentSystem.automations.BoundaryInteractable;
import Kurama.ComponentSystem.automations.HeightPercent;
import Kurama.ComponentSystem.automations.WidthPercent;
import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.components.Rectangle;
import Kurama.ComponentSystem.components.constraintGUI.interactionValidifiers.InteractionValidifier;
import Kurama.Math.Vector;
import Kurama.game.Game;

import java.util.ArrayList;
import java.util.List;

public class Boundary extends Rectangle {

    // in a H-Boundary, positive is top and negative is bottom.
    // in a V-boundary, positive is right and negative is left.

    public enum BoundaryOrient {Vertical, Horizontal};
    public static IVRequestPackGenerator defaultIVRGenerator = (parent, boundary, dx, dy) -> new BoundInteractionMessage(null, dx, dy);

    public BoundaryOrient boundaryOrient;

    public List<Boundary> negativeAttachments = new ArrayList<>();
    public List<Boundary> positiveAttachments = new ArrayList<>();
    public List<InteractionValidifier> preInteractionValidifiers = new ArrayList<>();
    public List<InteractionValidifier> postInteractionValidifiers = new ArrayList<>();
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

    public boolean shouldUpdateGridCell = false;

    public Boundary(Game game, Component parent, String identifier, BoundaryOrient orient) {
        this(game, parent, identifier, orient, null);
    }

    public Boundary(Game game, Component parent, String identifier, BoundaryOrient orient, BoundaryConfigurator configurator) {
        super(game, parent, identifier);

        this.boundaryOrient = orient;
        this.color = new Vector(1,1,1,1);

        this.addOnClickDraggedAction(new BoundaryInteractable(this)); // This will set delta move, and call relevant methods to move the boundary
//        this.addAutomation(updateBoundaryAutomation);

        if(boundaryOrient == BoundaryOrient.Horizontal) {
            this.setHeight(10);
            this.initAutomations.add(new WidthPercent(1));
        }
        else {
            this.setWidth(10);
            this.initAutomations.add(new HeightPercent(1));
        }

        if(configurator != null) {
            configurator.configure(this);
        }
    }

    public Boundary addPreInteractionValidifier(InteractionValidifier i) {
        preInteractionValidifiers.add(i);
        return this;
    }

    public Boundary addPostInteractionValidifier(InteractionValidifier i) {
        postInteractionValidifiers.add(i);
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

        var vd = new ConstraintVerificationData(getPos(), getWidth(), getHeight());

        for(var i: preInteractionValidifiers) {
            if(!i.isValid(this, info, vd)) {
                return false;
            }
        }
        return true;
    }

    public boolean isValidInteraction_post(BoundInteractionMessage info) {

        var vd = new ConstraintVerificationData(getPos(), getWidth(), getHeight());
        if(shouldUpdatePos) {
            vd.pos = updatedPos;
        }
        if(shouldUpdateWidth) {
            vd.width = (int) updatedWidth;
        }
        if(shouldUpdateHeight) {
            vd.height = (int) updatedHeight;
        }

        for(var i: postInteractionValidifiers) {
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
    }

    public boolean interact(BoundInteractionMessage info, Boundary parent, int relativePos) {

        var isValid = isValidInteraction_pre(info);

        if(isValid) {
            isValid = interactor.interact(info, this, parent, relativePos);
        }

        if(isValid) {
            isValid = isValidInteraction_post(info);
        }

        if(isValid) {

            if (shouldUpdatePos) {
                setPos(updatedPos);
            }
            if (shouldUpdateWidth) {
                setWidth((int) updatedWidth);
            }
            if (shouldUpdateHeight) {
                setHeight((int) updatedHeight);
            }
        }

        shouldUpdatePos = false;
        shouldUpdateWidth = false;
        shouldUpdateHeight = false;
        alreadyVisited = false;

        shouldUpdateGridCell = true;

        return isValid;
    }

}
