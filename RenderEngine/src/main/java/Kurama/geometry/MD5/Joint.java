package Kurama.geometry.MD5;

import Kurama.Math.Quaternion;
import Kurama.Math.Vector;

public class Joint {
    public String name;
    public int parent;
    public Vector pos;
    public Quaternion orient;
    public Vector scale;
    public int currentID = -1;

    public Joint(String name, int parent, Vector pos, Quaternion orient) {
        this.name = name;
        this.parent = parent;
        this.pos = pos;
        this.orient = orient;
        this.scale = new Vector(1,1,1);
    }

    public Joint(String name, int parent, Vector pos, Quaternion orient, Vector scale) {
        this.name = name;
        this.parent = parent;
        this.pos = pos;
        this.orient = orient;
        this.scale = scale;
    }
}