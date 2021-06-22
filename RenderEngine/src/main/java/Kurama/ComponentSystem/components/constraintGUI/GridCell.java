package Kurama.ComponentSystem.components.constraintGUI;

import Kurama.ComponentSystem.components.Component;
import Kurama.Math.Vector;
import Kurama.game.Game;
import Kurama.utils.Logger;

public class GridCell extends Component {

    public Component attachedComp;
    public Boundary top=null, bottom=null, right=null, left=null;
    private boolean isFirstRun = false;

    public GridCell(Game game, Component parent, String identifier) {
        super(game, parent, identifier);
        this.isContainerVisible = false;

        this.addInitAutomation((cur, in, timeDelta) -> updateAttachedComponent());

        addAutomation((curr, in, timeDelta) -> {
            if(top!=null && bottom!=null && right!=null && left!=null &&
                    (top.shouldUpdateGridCell || bottom.shouldUpdateGridCell || right.shouldUpdateGridCell || left.shouldUpdateGridCell)) {
                updateAttachedComponent();
            }
        });
    }

    public boolean integrateBorders() {

        if (top == null || bottom == null || right == null || left == null || attachedComp == null || !(attachedComp instanceof ConstraintComponent)) {
            return false;
        }

        ConstraintComponent constraintComponent = (ConstraintComponent) attachedComp;
        constraintComponent.isIntegratedWithParentBoundaries = true;

        // attaching comp.top's attachments to the parent bound
        {
            for (var b : constraintComponent.top.positiveAttachments) {
                if(b == constraintComponent.top || b == constraintComponent.bottom || b == constraintComponent.right || b == constraintComponent.left) continue;

                top.addConnectedBoundary(b, 1);
                constraintComponent.top.removeConnection(b);
                b.replaceConnection(constraintComponent.top, top);
            }
            for (var b : constraintComponent.top.negativeAttachments) {
                if(b == constraintComponent.top || b == constraintComponent.bottom || b == constraintComponent.right || b == constraintComponent.left) continue;

                top.addConnectedBoundary(b, 0);
                constraintComponent.top.removeConnection(b);
                b.replaceConnection(constraintComponent.top, top);
            }
            for (var b : constraintComponent.top.neutralAttachments) {
                if(b == constraintComponent.top || b == constraintComponent.bottom || b == constraintComponent.right || b == constraintComponent.left) continue;

                top.addConnectedBoundary(b, -1);
                constraintComponent.top.removeConnection(b);
                b.replaceConnection(constraintComponent.top, top);
            }
        }

        // attaching comp.bottom's attachments to the parent bound
        {
            for (var b : constraintComponent.bottom.positiveAttachments) {
                if(b == constraintComponent.top || b == constraintComponent.bottom || b == constraintComponent.right || b == constraintComponent.left) continue;

                bottom.addConnectedBoundary(b, 1);
                constraintComponent.bottom.removeConnection(b);
                b.replaceConnection(constraintComponent.bottom, bottom);
            }
            for (var b : constraintComponent.bottom.negativeAttachments) {
                if(b == constraintComponent.top || b == constraintComponent.bottom || b == constraintComponent.right || b == constraintComponent.left) continue;

                bottom.addConnectedBoundary(b, 0);
                constraintComponent.bottom.removeConnection(b);
                b.replaceConnection(constraintComponent.bottom, bottom);
            }
            for (var b : constraintComponent.bottom.neutralAttachments) {
                if(b == constraintComponent.top || b == constraintComponent.bottom || b == constraintComponent.right || b == constraintComponent.left) continue;

                bottom.addConnectedBoundary(b, -1);
                constraintComponent.bottom.removeConnection(b);
                b.replaceConnection(constraintComponent.bottom, bottom);
            }
        }

        // attaching comp.right's attachments to the parent bound
        {
            for (var b : constraintComponent.right.positiveAttachments) {
                if(b == constraintComponent.top || b == constraintComponent.bottom || b == constraintComponent.right || b == constraintComponent.left) continue;

                right.addConnectedBoundary(b, 1);
                constraintComponent.right.removeConnection(b);
                b.replaceConnection(constraintComponent.right, right);
            }
            for (var b : constraintComponent.right.negativeAttachments) {
                if(b == constraintComponent.top || b == constraintComponent.bottom || b == constraintComponent.right || b == constraintComponent.left) continue;

                right.addConnectedBoundary(b, 0);
                constraintComponent.right.removeConnection(b);
                b.replaceConnection(constraintComponent.right, right);
            }
            for (var b : constraintComponent.right.neutralAttachments) {
                if(b == constraintComponent.top || b == constraintComponent.bottom || b == constraintComponent.right || b == constraintComponent.left) continue;

                right.addConnectedBoundary(b, -1);
                constraintComponent.right.removeConnection(b);
                b.replaceConnection(constraintComponent.right, right);
            }
        }

        // attaching comp.left's attachments to the parent bound
        {
            for (var b : constraintComponent.left.positiveAttachments) {
                if(b == constraintComponent.top || b == constraintComponent.bottom || b == constraintComponent.right || b == constraintComponent.left) continue;

                left.addConnectedBoundary(b, 1);
                constraintComponent.left.removeConnection(b);
                b.replaceConnection(constraintComponent.left, left);
            }
            for (var b : constraintComponent.left.negativeAttachments) {
                if(b == constraintComponent.top || b == constraintComponent.bottom || b == constraintComponent.right || b == constraintComponent.left) continue;

                left.addConnectedBoundary(b, 0);
                constraintComponent.left.removeConnection(b);
                b.replaceConnection(constraintComponent.left, left);
            }
            for (var b : constraintComponent.left.neutralAttachments) {
                if(b == constraintComponent.top || b == constraintComponent.bottom || b == constraintComponent.right || b == constraintComponent.left) continue;

                left.addConnectedBoundary(b, -1);
                constraintComponent.left.removeConnection(b);
                b.replaceConnection(constraintComponent.left, left);
            }
        }

        constraintComponent.top_archive = constraintComponent.top;
        constraintComponent.bottom_archive = constraintComponent.bottom;
        constraintComponent.right_archive = constraintComponent.right;
        constraintComponent.left_archive = constraintComponent.left;

        constraintComponent.top.shouldTickRenderGroup = false;
        constraintComponent.bottom.shouldTickRenderGroup = false;
        constraintComponent.right.shouldTickRenderGroup = false;
        constraintComponent.left.shouldTickRenderGroup = false;

        constraintComponent.removeBoundary(constraintComponent.top)
                .removeBoundary(constraintComponent.bottom)
                .removeBoundary(constraintComponent.right)
                .removeBoundary(constraintComponent.left);

        constraintComponent.top = top;
        constraintComponent.bottom = bottom;
        constraintComponent.right = right;
        constraintComponent.left = left;

        updateAttachedComponent();

//        constraintComponent.addBoundary(constraintComponent.top)
//                .addBoundary(constraintComponent.bottom)
//                .addBoundary(constraintComponent.right)
//                .addBoundary(constraintComponent.left);

//        this.left.interact(new StretchMessage(0, 0, null, false), null, -1);
//        this.right.interact(new StretchMessage(0, 0, null, false), null, -1);
//        this.top.interact(new StretchMessage(0, 0, null, false), null, -1);
//        this.bottom.interact(new StretchMessage(0, 0, null, false), null, -1);


        return true;
    }


    // Method that is called when one of the boundaries have shouldUpdateGridCell=true.
    // Resizes the attached component

    public boolean updateAttachedComponent() {

        if(top == null || bottom == null || right == null || left == null || attachedComp == null) {
            var suffix = " Nulls: ";
            if(top == null) suffix+="top, ";
            if(bottom == null) suffix+="bottom, ";
            if(right == null) suffix+="right, ";
            if(left == null) suffix+="left, ";
            if(attachedComp == null) suffix+="attachedComp";

            Logger.logError("Boundaries and attached comp cannot be null: "+this.identifier + suffix);
            return false;
        }

        var topy = top.getPos().get(1) + top.getHeight() /2f;
        var bottomy = bottom.getPos().get(1) - bottom.getHeight() /2f;
        var rightx = right.getPos().get(0) - right.getWidth() /2f;
        var leftx = left.getPos().get(0) + left.getWidth() /2f;

        var newHeight = bottomy - topy;
        var newWidth = rightx - leftx;
        var newPos = new Vector(leftx + newWidth/2f, topy + newHeight/2f, 0);

        return attachedComp.resizeReposition(newPos, (int)newWidth, (int)newHeight);

//        attachedComp.setPos(newPos);
//        attachedComp.setWidth((int) newWidth);
//        attachedComp.setHeight((int) newHeight);

//        Logger.log("new dims of "+attachedComp.identifier + ": pos: "+attachedComp.getPos()+" width: "+attachedComp.getWidth() + " height: "+attachedComp.getHeight());

    }

}
