package Kurama.geometry.assimp;

import Kurama.Math.Quaternion;
import Kurama.Math.Vector;

public class Bone {

    public int boneId;
    public String boneName;
    public Vector pos;
    public Quaternion orient;

    public Bone(int boneId, String boneName, Vector pos, Quaternion orient) {
        this.boneId = boneId;
        this.boneName = boneName;
        this.pos = pos;
        this.orient = orient;
    }

}
