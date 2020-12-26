package Kurama.geometry.MD5;

import Kurama.Math.Quaternion;
import Kurama.Math.Vector;

public class Joint {
    public String name;
    public int parent;
    public Vector pos;
    public Quaternion orient;

    public Joint(String name, int parent, Vector pos, Quaternion orient) {
        this.name = name;
        this.parent = parent;
        this.pos = pos;
        this.orient = orient;
    }
}