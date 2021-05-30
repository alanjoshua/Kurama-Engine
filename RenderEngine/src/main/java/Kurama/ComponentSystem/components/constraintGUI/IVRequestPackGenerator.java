package Kurama.ComponentSystem.components.constraintGUI;

public interface IVRequestPackGenerator {

    public abstract BoundInteractionMessage getValidificationRequestPack(Boundary parent, Boundary boundary, float deltaMoveX, float deltaMoveY);

}
