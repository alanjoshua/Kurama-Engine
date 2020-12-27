package Kurama.Math;


// From this paper https://www.gamedevs.org/uploads/fast-extraction-viewing-frustum-planes-from-world-view-projection-matrix.pdf
// and the corresponding JOML implementation

public class FrustumIntersection {

    private float nxX, nxY, nxZ, nxW;
    private float pxX, pxY, pxZ, pxW;
    private float nyX, nyY, nyZ, nyW;
    private float pyX, pyY, pyZ, pyW;
    private float nzX, nzY, nzZ, nzW;
    private float pzX, pzY, pzZ, pzW;
    public Vector[] planes = new Vector[6];

    public FrustumIntersection() {
        for(int i = 0;i < planes.length; i++) {
            planes[i] = new Vector(4 ,0);
        }
    }

    public FrustumIntersection set(Matrix m) {
        float invl;
        nxX = m.get(3,0) + m.get(0,0); nxY = m.get(3,1) + m.get(0,1); nxZ = m.get(3,2) + m.get(0,2); nxW = m.get(3,3) + m.get(0,3);
        invl = invsqrt(nxX * nxX + nxY * nxY + nxZ * nxZ);
        nxX *= invl; nxY *= invl; nxZ *= invl; nxW *= invl;
        planes[0].setDataElement(0 ,nxX);
        planes[0].setDataElement(1 ,nxY);
        planes[0].setDataElement(2 ,nxZ);
        planes[0].setDataElement(3 ,nxW);


        pxX = m.get(3,0) - m.get(0,0); pxY = m.get(3,1) - m.get(0,1); pxZ = m.get(3,2) - m.get(0,2); pxW = m.get(3,3) - m.get(0,3);
        invl = invsqrt(pxX * pxX + pxY * pxY + pxZ * pxZ);
        pxX *= invl; pxY *= invl; pxZ *= invl; pxW *= invl;
        planes[1].setDataElement(0 ,pxX);
        planes[1].setDataElement(1 ,pxY);
        planes[1].setDataElement(2 ,pxZ);
        planes[1].setDataElement(3 ,pxW);

        nyX = m.get(3,0) + m.get(1,0); nyY = m.get(3,1) + m.get(1,1); nyZ = m.get(3,2) + m.get(1,2); nyW = m.get(3,3) + m.get(1,3);
            invl = invsqrt(nyX * nyX + nyY * nyY + nyZ * nyZ);
            nyX *= invl; nyY *= invl; nyZ *= invl; nyW *= invl;
        planes[2].setDataElement(0 ,nyX);
        planes[2].setDataElement(1 ,nyY);
        planes[2].setDataElement(2 ,nyZ);
        planes[2].setDataElement(3 ,nyW);

        pyX = m.get(3,0) - m.get(1,0); pyY = m.get(3,1) - m.get(1,1); pyZ = m.get(3,2) - m.get(1,2); pyW = m.get(3,3) - m.get(1,3);
        invl = invsqrt(pyX * pyX + pyY * pyY + pyZ * pyZ);
        pyX *= invl; pyY *= invl; pyZ *= invl; pyW *= invl;
        planes[3].setDataElement(0 ,pyX);
        planes[3].setDataElement(1 ,pyY);
        planes[3].setDataElement(2 ,pyZ);
        planes[3].setDataElement(3 ,pyW);

        nzX = m.get(3,0) + m.get(2,0); nzY = m.get(3,1) + m.get(2,1); nzZ = m.get(3,2) + m.get(2,2); nzW = m.get(3,3) + m.get(2,3);
        invl = invsqrt(nzX * nzX + nzY * nzY + nzZ * nzZ);
        nzX *= invl; nzY *= invl; nzZ *= invl; nzW *= invl;
        planes[4].setDataElement(0 ,nzX);
        planes[4].setDataElement(1 ,nzY);
        planes[4].setDataElement(2 ,nzZ);
        planes[4].setDataElement(3 ,nzW);

        pzX = m.get(3,0) - m.get(2,0); pzY = m.get(3,1) - m.get(2,1); pzZ = m.get(3,2) - m.get(2,2); pzW = m.get(3,3) - m.get(2,3);
        invl = invsqrt(pzX * pzX + pzY * pzY + pzZ * pzZ);
        pzX *= invl; pzY *= invl; pzZ *= invl; pzW *= invl;
        planes[5].setDataElement(0 ,pzX);
        planes[5].setDataElement(1 ,pzY);
        planes[5].setDataElement(2 ,pzZ);
        planes[5].setDataElement(3 ,pzW);

        return this;
    }

    public boolean testSphere(Vector center, float radius) {
        return testSphere(center.get(0), center.get(1), center.get(2), radius);
    }

    public boolean testSphere(float x, float y, float z, float r) {
        return nxX * x + nxY * y + nxZ * z + nxW >= -r &&
                pxX * x + pxY * y + pxZ * z + pxW >= -r &&
                nyX * x + nyY * y + nyZ * z + nyW >= -r &&
                pyX * x + pyY * y + pyZ * z + pyW >= -r &&
                nzX * x + nzY * y + nzZ * z + nzW >= -r &&
                pzX * x + pzY * y + pzZ * z + pzW >= -r;
    }

    public static float invsqrt(float r) {
        return 1.0f / (float) java.lang.Math.sqrt(r);
    }

}
