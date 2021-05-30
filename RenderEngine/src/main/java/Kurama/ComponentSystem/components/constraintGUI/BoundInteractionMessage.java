package Kurama.ComponentSystem.components.constraintGUI;

public class BoundInteractionMessage {

    public float deltaMoveX;
    public float deltaMoveY;
    public int parentMoveDir = -1;
    public Boundary parent;

    public BoundInteractionMessage(float deltaMoveX, float deltaMoveY) {
        this.deltaMoveX = deltaMoveX;
        this.deltaMoveY = deltaMoveY;
        this.parent = null;
    }

    public BoundInteractionMessage(Boundary parent, float deltaMoveX, float deltaMoveY) {
        this.deltaMoveX = deltaMoveX;
        this.deltaMoveY = deltaMoveY;
        this.parent = parent;
    }

    public BoundInteractionMessage(Boundary parent, float deltaMoveX, float deltaMoveY, int parentMoveDir) {
        this.parent = parent;
        this.deltaMoveX = deltaMoveX;
        this.deltaMoveY = deltaMoveY;
        this.parentMoveDir = parentMoveDir;
    }

    public BoundInteractionMessage(BoundInteractionMessage mess) {
        this.deltaMoveX = mess.deltaMoveX;
        this.deltaMoveY = mess.deltaMoveY;
        this.parentMoveDir = mess.parentMoveDir;
        this.parent = mess.parent;
    }

    public String toString() {
        return "dx: "+deltaMoveX+" dy: "+deltaMoveY + " parentMovedir: "+parentMoveDir + " parentID: " + (parent==null?"null": parent.identifier);
    }

}
