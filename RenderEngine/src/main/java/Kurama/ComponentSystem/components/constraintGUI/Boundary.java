package Kurama.ComponentSystem.components.constraintGUI;

import Kurama.ComponentSystem.automations.BoundaryInteractable;
import Kurama.ComponentSystem.automations.HeightPercent;
import Kurama.ComponentSystem.automations.WidthPercent;
import Kurama.ComponentSystem.components.Component;
import Kurama.ComponentSystem.components.Rectangle;
import Kurama.ComponentSystem.components.constraintGUI.interactionValidifiers.InteractionValidifier;
import Kurama.Math.Vector;
import Kurama.game.Game;
import Kurama.utils.Logger;

import java.util.ArrayList;
import java.util.List;

public class Boundary extends Rectangle {

    // in a H-Boundary, positive is top and 0 is bottom.
    // in a V-boundary, positive is right and 0 is left.

    public enum BoundaryOrient {Vertical, Horizontal};
    public static IVRequestPackGenerator defaultIVRGenerator = (parent, boundary, dx, dy) -> new BoundInteractionMessage(null, dx, dy);

    public BoundaryOrient boundaryOrient;

    public List<Boundary> negativeAttachments = new ArrayList<>();
    public List<Boundary> positiveAttachments = new ArrayList<>();
    public List<Boundary> neutralAttachments = new ArrayList<>();
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

    public float minWidth = Float.NEGATIVE_INFINITY;
    public float maxWidth = Float.POSITIVE_INFINITY;
    public float minHeight = Float.NEGATIVE_INFINITY;
    public float maxHeight = Float.POSITIVE_INFINITY;

    public boolean shouldUpdateGridCell = false;
    protected boolean isUserInteractable = false;

    public Boundary(Game game, Component parent, String identifier, BoundaryOrient orient, boolean userInteractable) {
        this(game, parent, identifier, orient,userInteractable,null);
    }

    public Boundary(Game game, Component parent, String identifier, BoundaryOrient orient, boolean userInteractable, BoundaryConfigurator configurator) {
        super(game, parent, identifier);

        this.boundaryOrient = orient;
        this.color = new Vector(1,1,1,1);

        this.addOnResizeAction((curr, in, t) -> ((Boundary)curr).shouldUpdateGridCell=true);

        if(userInteractable) {
            makeInteractable();
        }
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

    public void makeInteractable() {
        if(isUserInteractable) return;

        this.addOnClickDraggedAction(new BoundaryInteractable(this)); // This will set delta move, and call relevant methods to move the boundary
        isUserInteractable = true;
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
        else if(connectionType == 1) {
            positiveAttachments.add(connection);
        }
        else {
            neutralAttachments.add(connection);
        }
        return this;
    }

    public void removeConnection(Boundary bound) {
        positiveAttachments.remove(bound);
        negativeAttachments.remove(bound);
        neutralAttachments.remove(bound);
    }

    public void replaceConnection(Boundary boundToBeReplaced, Boundary newBound) {

        if(positiveAttachments.contains(boundToBeReplaced)) {
            positiveAttachments.remove(boundToBeReplaced);
            positiveAttachments.add(newBound);
        }

        if(negativeAttachments.contains(boundToBeReplaced)) {
            negativeAttachments.remove(boundToBeReplaced);
            negativeAttachments.add(newBound);
        }

        if(neutralAttachments.contains(boundToBeReplaced)) {
            neutralAttachments.remove(boundToBeReplaced);
            neutralAttachments.add(newBound);
        }
    }

    public Boundary addConnectedBoundary(Boundary connection, int connectionType, int reverseConnectionType) {

        if(connectionType == 0) {
            negativeAttachments.add(connection);
        }
        else if(connectionType == 1) {
            positiveAttachments.add(connection);
        }
        else {
            neutralAttachments.add(connection);
        }

        connection.addConnectedBoundary(this, reverseConnectionType);
        return this;
    }

    public boolean isValidInteraction_pre(BoundInteractionMessage info) {

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

    public void recursiveAlreadyVisitedTurnOff() {
        if(alreadyVisited == false) return;

        this.alreadyVisited = false;

        for(var b: positiveAttachments) {
            b.recursiveAlreadyVisitedTurnOff();
        }
        for(var b: negativeAttachments) {
            b.recursiveAlreadyVisitedTurnOff();
        }
        for(var b: neutralAttachments) {
            b.recursiveAlreadyVisitedTurnOff();
        }
    }

    public boolean initialiseInteraction(float deltaMoveX, float deltaMoveY) {
        var data = IVRequestPackGenerator.getValidificationRequestPack(null,this, deltaMoveX, deltaMoveY);
        return interact(data, null, -1);
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
        else {
            Logger.logError(identifier + " boundary not valid interaction");
        }

        shouldUpdatePos = false;
        shouldUpdateWidth = false;
        shouldUpdateHeight = false;
//        alreadyVisited = false;

        return isValid;
    }

}
