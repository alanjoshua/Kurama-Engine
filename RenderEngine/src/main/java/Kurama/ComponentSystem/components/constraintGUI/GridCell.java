package Kurama.ComponentSystem.components.constraintGUI;

import Kurama.ComponentSystem.components.Component;

public class GridCell {

    public Component attachedComp;
    public Boundary top=null, bottom=null, right=null, left=null;

    public GridCell() {

    }

    // Method that is called when one of the boundaries have shouldUpdateGridCell=true.
    // Resizes the attached component

    public void updateAttachedComponent() {

    }

}
