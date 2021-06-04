package Kurama.ComponentSystem.components;

import Kurama.Math.Vector;
import Kurama.game.Game;
import Kurama.geometry.Utils;
import Kurama.inputs.Input;

public class Rectangle extends Component {

    public Vector radii;

    public Vector texUL = new Vector(new float[]{0,0, 0, 0});
    public Vector texBL = new Vector(new float[]{0, 1, 0, 0});
    public Vector texUR = new Vector(new float[]{1, 0, 0, 0});
    public Vector texBR = new Vector(new float[]{1,1, 0, 0});

    private static Vector v1 = new Vector(-0.5f,-0.5f,0f,1f);
    private static Vector v2 =  new Vector(0.5f,-0.5f,0f,1f);
    private static Vector v3 =  new Vector(-0.5f,0.5f,0f,1f);
    private static Vector v4 =  new Vector(0.5f,0.5f,0f,1f);

    public Rectangle(Game game, Component parent, Vector radii, String identifier) {
        super(game, parent, identifier);
        this.radii = radii;
    }

    public Rectangle(Game game, Component parent, String identifier) {
        super(game, parent, identifier);
        this.radii = new Vector(0,0,0,0);
        texUL = new Vector(new float[]{0,0, 0, 0});
        texBL = new Vector(new float[]{0, 1, 0, 0});
        texUR = new Vector(new float[]{1, 0, 0, 0});
        texBR = new Vector(new float[]{1,1, 0, 0});
    }

    public Rectangle setRadii(Vector radii) {
        this.radii = radii;
        return this;
    }

    @Override
    public boolean isMouseOverComponent(Input input) {
            if(input != null && input.isCursorEnabled) {
                var mp = input.getPos().append(0);

                var topLeft = objectToWorldMatrix.vecMul(v1).removeDimensionFromVec(3).removeDimensionFromVec(2);
                var topRight = objectToWorldMatrix.vecMul(v2).removeDimensionFromVec(3).removeDimensionFromVec(2);
                var bottomLeft = objectToWorldMatrix.vecMul(v3).removeDimensionFromVec(3).removeDimensionFromVec(2);
                var bottomRight = objectToWorldMatrix.vecMul(v4).removeDimensionFromVec(3).removeDimensionFromVec(2);

                return Utils.isPointInsideRectangle2D(mp, topLeft, topRight, bottomLeft, bottomRight);
            }
            else {
                return false;
            }
        }

}
