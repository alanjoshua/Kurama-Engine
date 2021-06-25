package Kurama.ComponentSystem.components.constraintGUI;

import Kurama.ComponentSystem.components.Component;
import Kurama.Math.Vector;
import Kurama.game.Game;
import Kurama.utils.Logger;

import java.util.ArrayList;
import java.util.List;

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

        ConstraintComponent comp = ((ConstraintComponent) attachedComp);

        if(!comp.isIntegratedWithParentBoundaries) {
            comp.integrateWithMasterConstraintSystem(this, (ConstraintComponent) this.parent);
            updateAttachedComponent();
            Logger.logError("Finished Intergrating bounds");
        }

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

        // Since pos is relative to parent, if the component is integrated, then the pos would be wrong
        // This assumes gridcells are properly arranged in hierarchical order.
        // This also assumes all boundaries are in the same hierarchy level

        if(top.parent != attachedComp.parent) {
            List<Component> parentChain = new ArrayList<>();
            var curParent = attachedComp.parent;

            while(curParent != top.parent) {
                parentChain.add(curParent);
                curParent = curParent.parent;
            }

            for(var curParent2: parentChain) {
                newPos = newPos.sub(curParent2.pos);
            }
        }

        return attachedComp.resizeReposition(newPos, (int)newWidth, (int)newHeight);

//        attachedComp.setPos(newPos);
//        attachedComp.setWidth((int) newWidth);
//        attachedComp.setHeight((int) newHeight);

//        Logger.log("new dims of "+attachedComp.identifier + ": pos: "+attachedComp.getPos()+" width: "+attachedComp.getWidth() + " height: "+attachedComp.getHeight());

    }

}
