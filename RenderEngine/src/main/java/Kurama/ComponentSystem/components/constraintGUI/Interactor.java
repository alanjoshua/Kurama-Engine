package Kurama.ComponentSystem.components.constraintGUI;

public interface Interactor {

    public abstract boolean interact(BoundInteractionMessage info, Boundary boundary, Boundary parentBoundary, int relativePos);

}
