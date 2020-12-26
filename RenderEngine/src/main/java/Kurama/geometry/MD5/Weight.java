package Kurama.geometry.MD5;

import Kurama.Math.Vector;

public class Weight {

    public int index;
    public int joint;
    public float bias;
    public Vector pos;

    public Weight(int index, int joint, float bias, Vector pos) {
        this.index = index;
        this.joint = joint;
        this.bias = bias;
        this.pos = pos;
    }

}
