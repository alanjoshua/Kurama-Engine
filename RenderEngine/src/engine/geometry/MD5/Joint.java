package engine.geometry.MD5;

import engine.Math.Quaternion;
import engine.Math.Vector;

public class Joint {
    public String name;
    public int parent;
    public Vector pos;
    public Quaternion orient;

    public Joint(String name, int parent, Vector pos, Vector orient) {
        this.name = name;
        this.parent = parent;
        this.pos = pos;

        float t = 1f - (orient.get(0) * orient.get(0)) - (orient.get(1) * orient.get(1)) - (orient.get(2) * orient.get(2));
        float w;
        if(t < 0f) {
            w = 0f;
        }
        else {
            w = (float) -Math.sqrt(t);
        }
        this.orient = new Quaternion(new Vector(w, orient.get(0), orient.get(1), orient.get(2)));
    }
}