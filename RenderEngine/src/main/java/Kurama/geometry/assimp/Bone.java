package Kurama.geometry.assimp;

import Kurama.Math.Matrix;

public class Bone {

    public int boneId;
    public String boneName;
    public Matrix unbindMatrix;
    public String parentName = null;

    public Bone(int boneId, String boneName, Matrix unbindMatrix) {
        this.boneId = boneId;
        this.boneName = boneName;
        this.unbindMatrix = unbindMatrix;
    }

}
