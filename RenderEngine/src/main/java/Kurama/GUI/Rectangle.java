package Kurama.GUI;

import Kurama.Math.Vector;

public class Rectangle extends Component {

    public Vector radii;

    public Rectangle(Component parent, Vector radii, String identifier) {
        super(parent, identifier);
        this.radii = radii;
    }

    public Rectangle(Component parent, String identifier) {
        super(parent, identifier);
        this.radii = new Vector(0,0,0,0);
    }

}
