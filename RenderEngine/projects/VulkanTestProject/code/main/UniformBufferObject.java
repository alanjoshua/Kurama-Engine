package main;

import org.joml.Matrix4f;

public class UniformBufferObject {

    public static final int SIZEOF = 3 * 16 * Float.BYTES;

    public Matrix4f model;
    public Matrix4f view;
    public Matrix4f proj;

    public UniformBufferObject() {
        model = new Matrix4f();
        view = new Matrix4f();
        proj = new Matrix4f();
    }

}
