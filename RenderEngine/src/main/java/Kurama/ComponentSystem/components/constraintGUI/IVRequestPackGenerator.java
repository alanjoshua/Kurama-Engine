package Kurama.ComponentSystem.components.constraintGUI;

public interface IVRequestPackGenerator {

    public abstract BoundMoveDataPack getValidificationRequestPack(Boundary boundary, float deltaMoveX, float deltaMoveY);

}
