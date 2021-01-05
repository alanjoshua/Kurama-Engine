package Kurama.GUI;

public class HeightPercent extends Constraint {

    public float heightPercent;

    public HeightPercent(float heightPercent) {
        this.heightPercent = heightPercent;
    }

    @Override
    public void solveConstraint(Component parent, Component current) {
        current.height = (int)(parent.height * heightPercent);
    }
}
