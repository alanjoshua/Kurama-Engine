package Kurama.ComponentSystem.components.constraintGUI.interactionConstraints;

import Kurama.ComponentSystem.components.constraintGUI.BoundMoveDataPack;
import Kurama.ComponentSystem.components.constraintGUI.Boundary;
import Kurama.utils.Logger;

public class MaxXPos implements InteractionConstraint {

    public float maxXPos;

    // relative to parent
    public MaxXPos(float maxPos) {
        maxXPos = maxPos;
    }

    @Override
    public boolean isValid(Boundary boundary, BoundMoveDataPack info) {

        float curX = boundary.objectToWorldMatrix.getColumn(3).get(0) + boundary.width/2f;


//        if(curX+info.deltaMove >= maxXPos) {
//            return false;
//        }
//        else {
//            return true;
//        }

        float cur = boundary.pos.get(0) + boundary.width/2f + boundary.parent.width/2f + info.deltaMove;
        float max = boundary.parent.width * maxXPos;

        if(boundary.identifier.equals("rr")) {
            Logger.log("max: " + max + " cur: " + cur);
        }
        if(cur >= max) {
            return false;
        }
        else {
            return true;
        }

//        float parentInitPos = boundary.parent.objectToWorldMatrix.getColumn(3).get(0) - boundary.parent.width/2f;
//
//        float max = (maxPercent * boundary.parent.width);
//        float cur = cur = boundary.pos.geti(0) + boundary.width/2f + info.deltaMove;
//

//
//        if(cur > max) {
//            return false;
//        }
//        else {
//            return true;
//        }
////        return true;
    }
}
