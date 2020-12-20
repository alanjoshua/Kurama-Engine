package engine.Effects;

import engine.Math.Vector;

public class Fog {

    public boolean active = false;
    public Vector color = new Vector(new float[]{0,0,0});
    public float density = 0;
    public static Fog NOFOG = new Fog();

    public Fog() {}

    public Fog(boolean active, Vector color, float density) {
        this.active = active;
        this.color = color;
        this.density = density;
    }

}
