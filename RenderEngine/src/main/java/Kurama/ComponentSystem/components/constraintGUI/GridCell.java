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


    // Method that is called when one of the boundaries have shouldUpdateGridCell=true.
    // Resizes the attached component

    public void updateAttachedComponent() {

        if(top == null || bottom == null || right == null || left == null || attachedComp == null) {
            var suffix = " Nulls: ";
            if(top == null) suffix+="top, ";
            if(bottom == null) suffix+="bottom, ";
            if(right == null) suffix+="right, ";
            if(left == null) suffix+="left, ";
            if(attachedComp == null) suffix+="attachedComp";

            Logger.logError("Boundaries and attached comp cannot be null: "+this.identifier + suffix);
            return;
        }

        var topy = top.getPos().get(1) + top.getHeight() /2f;
        var bottomy = bottom.getPos().get(1) - bottom.getHeight() /2f;
        var rightx = right.getPos().get(0) - right.getWidth() /2f;
        var leftx = left.getPos().get(0) + left.getWidth() /2f;

        var newHeight = bottomy - topy;
        var newWidth = rightx - leftx;
        attachedComp.setPos(new Vector(leftx + newWidth/2f, topy + newHeight/2f, 0));
        attachedComp.setWidth((int) newWidth);
        attachedComp.setHeight((int) newHeight);

        Logger.log("new dims of "+attachedComp.identifier + ": pos: "+attachedComp.getPos()+" width: "+attachedComp.getWidth() + " height: "+attachedComp.getHeight());

    }

}
