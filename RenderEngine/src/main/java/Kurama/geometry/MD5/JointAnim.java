package Kurama.geometry.MD5;

import Kurama.Math.Quaternion;
import Kurama.Math.Vector;

public class JointAnim {

    public String name;
    public int parent;
    public int flags;
    public int startIndex;
    public Vector base_pos;
    public Quaternion base_orient;

    public JointAnim(String name, int parent, int flags, int startIndex) {
        this.name = name;
        this.parent = parent;
        this.flags = flags;
        this.startIndex = startIndex;
    }

}
