package main;

import Kurama.Math.Matrix;
import org.joml.Matrix4f;

public class UniformBufferObject {

    public static final int SIZEOF = 3 * 16 * Float.BYTES;

    public Matrix model;
    public Matrix view;
    public Matrix proj;

    public UniformBufferObject() {
        model = Matrix.getIdentityMatrix(4);
        view = Matrix.getIdentityMatrix(4);
        proj = Matrix.getIdentityMatrix(4);
    }

}
