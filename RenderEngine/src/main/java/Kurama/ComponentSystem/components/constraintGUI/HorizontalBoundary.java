package Kurama.ComponentSystem.components.constraintGUI;

import Kurama.ComponentSystem.automations.WidthPercent;
import Kurama.ComponentSystem.components.Component;
import Kurama.Math.Vector;
import Kurama.game.Game;

public class HorizontalBoundary extends Boundary {

    public HorizontalBoundary(Game game, Component parent, String identifier) {
        super(game, parent, identifier);
        this.color = new Vector(1,1,1,1);
        this.height = 10;
        this.initAutomations.add(new WidthPercent(1));
    }

    @Override
    public boolean canBeMoved(BoundMoveDataPack info) {
        return true;
    }

    public void move(BoundMoveDataPack info, Boundary parent, int parentMoveDir) {

        if(parent == null) {
            pos = pos.add(new Vector(0, (int) info.deltaMove, 0));
            parentMoveDir = 1;
        }
        else {
            if(parentMoveDir == 0) {
                pos = pos.add(new Vector((int) info.deltaMove,0, 0));
            }
            else {
                pos = pos.add(new Vector(0, (int) info.deltaMove, 0));
            }
        }

        this.alreadyUpdated = true;
        int temp = parentMoveDir;

        negativeAttachments.forEach(b -> {
            if(!b.alreadyUpdated)
                b.move(info, this, temp);
        });
        positiveAttachments.forEach(b -> {
            if(!b.alreadyUpdated)
                b.move(info, this, temp);
        });
    }
}
