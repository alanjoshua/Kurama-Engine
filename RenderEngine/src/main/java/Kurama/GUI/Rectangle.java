package Kurama.GUI;

import Kurama.Math.Vector;

public class Rectangle extends Component {

    public Vector radii;

    public Vector texUL = new Vector(new float[]{0,0});
    public Vector texBL = new Vector(new float[]{0, 1});
    public Vector texUR = new Vector(new float[]{1, 0});
    public Vector texBR = new Vector(new float[]{1,1});

    public Rectangle(Component parent, Vector radii, String identifier) {
        super(parent, identifier);
        this.radii = radii;
    }

    public Rectangle(Component parent, String identifier) {
        super(parent, identifier);
        this.radii = new Vector(0,0,0,0);
        texUL = new Vector(new float[]{0,0});
        texBL = new Vector(new float[]{0, 1});
        texUR = new Vector(new float[]{1, 0});
        texBR = new Vector(new float[]{1,1});
    }

}
