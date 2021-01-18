package Kurama.GUI.constraints;

import Kurama.GUI.components.Component;

public class Fit implements Constraint {

    @Override
    public void solveConstraint(Component parent, Component current) {
        float aspectRatio = (float) current.width / (float) current.height;

        if(aspectRatio < 1) { // width is less than height
            current.height = parent.height;
            current.width = (int) ((float)current.height * aspectRatio);
        }
        else {
            current.width = parent.width;
            current.height = (int)((float)current.width / aspectRatio);
        }

    }
}
