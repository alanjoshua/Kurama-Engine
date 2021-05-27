package Kurama.ComponentSystem.components.constraintGUI;

public class BoundMoveDataPack {

    public float deltaMoveX;
    public float deltaMoveY;
    public int parentMoveDir = -1;

    public BoundMoveDataPack(float deltaMoveX, float deltaMoveY) {
        this.deltaMoveX = deltaMoveX;
        this.deltaMoveY = deltaMoveY;
    }

    public BoundMoveDataPack(float deltaMoveX, float deltaMoveY, int parentMoveDir) {
        this.deltaMoveX = deltaMoveX;
        this.deltaMoveY = deltaMoveY;
        this.parentMoveDir = parentMoveDir;
    }
}
