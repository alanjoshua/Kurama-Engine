package Kurama.camera;

import Kurama.ComponentSystem.components.Component;
import Kurama.Math.Matrix;
import Kurama.Math.Quaternion;
import Kurama.Math.Vector;
import Kurama.game.Game;
import Kurama.renderingEngine.RenderingEngine;

import static java.lang.Math.max;
import static java.lang.Math.tan;

public class StereoCamera extends Camera {

    public float eyeSeparation;
    public Matrix leftProjection;
    public Matrix rightProjection;
    public Matrix leftObjectToWorld;
    public Matrix rightObjectToWorld;
    public Matrix leftWorldToCam;
    public Matrix rightWorldToCam;
    public float focalLength = 0.5f;

    public StereoCamera(Game game, Component parent, Quaternion quaternion, Vector pos, float fovX, float nearClippingPlane, float farClippingPlane, int imageWidth, int imageHeight, boolean shouldUseRenderBuffer, String identifier) {
        super(game, parent, quaternion, pos, fovX, nearClippingPlane, farClippingPlane, imageWidth, imageHeight, shouldUseRenderBuffer, identifier);
    }

    public void loadDefaultSettings() {
        focalLength = 1;
        // Best practises for optimal 3d experience
        eyeSeparation = focalLength / 30.0f;
        nearClippingPlane = focalLength / 5.0f;
    }

    @Override
    public void updateValues() {
        int imageWidth = renderResolution.geti(0);
        int imageHeight = renderResolution.geti(1);

        if(shouldUseRenderBuffer) {
            renderBuffer.resizeTexture(renderResolution);
        }

        //Reference from https://github.com/SaschaWillems/Vulkan/blob/master/examples/multiview/multiview.cpp
        // and http://paulbourke.net/stereographics/stereorender/

        var projMatrices =
                createStereoProjectionMatrices(imageWidth, imageHeight,
                fovX, eyeSeparation, focalLength,
                nearClippingPlane, farClippingPlane);

        leftProjection = projMatrices[0];
        rightProjection = projMatrices[1];

        Matrix rotationMatrix = getOrientation().getRotationMatrix();
        Matrix scalingMatrix = Matrix.getDiagonalMatrix(getScale());
        Matrix rotScalMatrix = rotationMatrix.matMul(scalingMatrix);

        Vector cameraRight = rotationMatrix.getColumn(0).normalise();

        leftObjectToWorld = rotScalMatrix.
                addColumn(getPos().sub(cameraRight.scalarMul(eyeSeparation/2f))).
                addRow(new Vector(new float[]{0, 0, 0, 1}));

        rightObjectToWorld = rotScalMatrix.
                addColumn(getPos().add(cameraRight.scalarMul(eyeSeparation/2f))).
                addRow(new Vector(new float[]{0, 0, 0, 1}));

        leftWorldToCam = leftObjectToWorld.getInverse();
        rightWorldToCam = rightObjectToWorld.getInverse();
    }

    public static Matrix[] createStereoProjectionMatrices(int width, int height, float fov, float eyeSeparation, float focalLength, float zNear, float zFar) {
        Matrix[] matrices = new Matrix[2];

        // Calculate some variables
        float aspectRatio = (width) / (float)height;
        float wd2 = (float) (zNear * tan(Math.toRadians(fov / 2.0f)));
        float ndfl = zNear / focalLength;
        float left, right;
        float top = wd2;
        float bottom = -wd2;

        // Left eye
        left = -aspectRatio * wd2 + 0.5f * eyeSeparation * ndfl;
        right = aspectRatio * wd2 + 0.5f * eyeSeparation * ndfl;

        matrices[0] = Matrix.buildPerspectiveProjectionMatrix(zNear, zFar, left, right, top, bottom);

        // Right eye
        left = -aspectRatio * wd2 - 0.5f * eyeSeparation * ndfl;
        right = aspectRatio * wd2 - 0.5f * eyeSeparation * ndfl;

        matrices[1] = Matrix.buildPerspectiveProjectionMatrix(zNear, zFar, left, right, top, bottom);

        return matrices;
    }

}
