package main;

import Kurama.Math.Matrix;
import Kurama.Math.Vector;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

public class MeshPushConstants {

    public static int SIZEOF = (16 + 4) * Float.BYTES;
    public Vector data;
    public Matrix renderMatrix;

    public MeshPushConstants() {
        renderMatrix = Matrix.getIdentityMatrix(4);
        data = new Vector(4, 0);
    }

    public FloatBuffer getAsFloatBuffer() {
        FloatBuffer res = MemoryUtil.memAllocFloat((4 * 4 * 4) + (4 * 4));

        for(float v: data.getData()) {
            res.put(v);
        }

        for(Vector c:renderMatrix.convertToColumnVectorArray()) {
            for(float val: c.getData()) {
                res.put(val);
            }
        }
        res.flip();
        return res;
    }

}
