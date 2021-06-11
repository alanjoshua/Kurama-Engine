package Kurama.ComponentSystem.components.constraintGUI;

import Kurama.ComponentSystem.components.Component;
import Kurama.Math.Vector;
import Kurama.game.Game;
import Kurama.utils.Logger;

public class GridCell extends Component {

    public Component attachedComp;
    public Boundary top=null, bottom=null, right=null, left=null;

    public GridCell(Game game, Component parent, String identifier) {
        super(game, parent, identifier);

        addAutomation((curr, in, timeDelta) -> {

            if(top!=null && top.shouldUpdateGridCell) {
                updateAttachedComponent();
                top.shouldUpdateGridCell = false;
            }
            else if(bottom!=null && bottom.shouldUpdateGridCell) {
                updateAttachedComponent();
                top.shouldUpdateGridCell = false;
            }
            else if(right!=null && right.shouldUpdateGridCell) {
                updateAttachedComponent();
                top.shouldUpdateGridCell = false;
            }
            else if(left!=null && left.shouldUpdateGridCell) {
                updateAttachedComponent();
                top.shouldUpdateGridCell = false;
            }

        });
    }


    // Method that is called when one of the boundaries have shouldUpdateGridCell=true.
    // Resizes the attached component

    public void updateAttachedComponent() {
        if(top == null || bottom == null || right == null || left == null || attachedComp == null) {
            Logger.logError("Boundaries and attached comp cannot be null: "+this.identifier);
            return;
        }

        var topy = top.pos.get(1)-top.height/2f;
        var bottomy = bottom.pos.get(1)+bottom.height/2f;
        var rightx = right.pos.get(1)-right.width/2f;
        var leftx = left.pos.get(1)+left.width/2f;

        var newHeight = bottomy - topy;
        var newWidth = rightx - leftx;
        attachedComp.pos = new Vector(leftx + newWidth/2f, topy + newHeight/2f, 0);
        attachedComp.width = (int) newWidth;
        attachedComp.height = (int) newHeight;

    }

}
