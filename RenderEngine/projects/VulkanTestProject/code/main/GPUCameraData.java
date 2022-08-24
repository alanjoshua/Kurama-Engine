package main;

import Kurama.Math.Matrix;

public class GPUCameraData {

    public static final int SIZEOF = 3 * 16 * Float.BYTES;

    public Matrix projview;
    public Matrix view;
    public Matrix proj;

    public GPUCameraData() {
        projview = Matrix.getIdentityMatrix(4);
        view = Matrix.getIdentityMatrix(4);
        proj = Matrix.getIdentityMatrix(4);
    }

}
