package rendering;

import Math.Matrix;
import Math.Vector;
import main.Game;
import models.Model;

public class Camera {

	private Matrix camMatrix;
	private Matrix camMatrixInverse;

	private Game game;

	private float filmApertureWidth;
	private float filmApertureHeight;
	private float focalLength;
	private float nearClippingPlane;
	private float farClippingPlane;
	private int imageWidth = 0;
	private int imageHeight = 0;

	private float fovX;
	private float fovY;
	private float canvasWidth;
	private float canvasHeight;
	private float right, left, top, bottom;

	public static final int FILL = 0;
	public static final int OVERSCAN = 1;
	private int fitMode = 0;
	private float filmAspectRatio = 1;
	private float deviceAspectRatio = 1;
	private float xScale = 1;
	private float yScale = 1;

	private int cameraMode = 0;
	public static final int simulateTrueCamera = 1;
	public static final int gameModeCamera = 0;
	
	private Matrix perspectiveProjectionMatrix = null;
	private Matrix orthographicProjectionMatrix = null;

	private final float inchToMm = 25.4f;

	public Camera(Game game, float[][] data, float focalLength, float filmApertureWidth, float filmApertureHeight,
			float nearClippingPlane, float farClippingPlane, int imageWidht, int imageHeight, int cameraMode) {
		this.camMatrix = new Matrix(data);
		this.game = game;
		this.filmApertureWidth = filmApertureWidth;
		this.filmApertureHeight = filmApertureHeight;
		this.focalLength = focalLength;
		this.nearClippingPlane = nearClippingPlane;
		this.farClippingPlane = farClippingPlane;
		this.imageWidth = imageWidht;
		this.imageHeight = imageHeight;
		this.cameraMode = cameraMode;

		canvasWidth = 0;
		canvasHeight = 0;

		computeInverse();
		updateValues();
	}
	
	public Camera(Game game, float[][] data, float fovX,float nearClippingPlane, float farClippingPlane, int imageWidht, int imageHeight, int cameraMode) {
		this.camMatrix = new Matrix(data);
		this.game = game;
		this.filmApertureWidth = 0;
		this.filmApertureHeight = 0;
		this.focalLength = 0;
		this.nearClippingPlane = nearClippingPlane;
		this.farClippingPlane = farClippingPlane;
		this.imageWidth = imageWidht;
		this.imageHeight = imageHeight;
		this.cameraMode = cameraMode;
		this.fovX = fovX;
		
		canvasWidth = 0;
		canvasHeight = 0;

		computeInverse();
		updateValues();
	}

	public void tick() {
		
	}

	public void updateValues() {

		if (cameraMode == simulateTrueCamera) {
			fovX = (float) (2 * Math.atan((filmApertureWidth * inchToMm * 0.5) / focalLength));
			fovY = (float) (2 * Math.atan((filmApertureHeight * inchToMm * 0.5) / focalLength));
			canvasWidth = (float) (2 * ((filmApertureWidth * inchToMm * 0.5) / focalLength) * nearClippingPlane);
			canvasHeight = (float) (2 * ((filmApertureHeight * inchToMm * 0.5) / focalLength) * nearClippingPlane);
			right = canvasWidth / 2f;
			left = -right;
			top = canvasHeight / 2f;
			bottom = -top;

			xScale = 1;
			yScale = 1;

			filmAspectRatio = filmApertureWidth / filmApertureHeight;
			deviceAspectRatio = imageWidth / (float) imageHeight;

			switch (fitMode) {
			case FILL:
				if (filmAspectRatio > deviceAspectRatio) {
					// 8a
					xScale = deviceAspectRatio / filmAspectRatio;
				} else {
					// 8c
					yScale = filmAspectRatio / deviceAspectRatio;
				}
				break;
			case OVERSCAN:
				if (filmAspectRatio > deviceAspectRatio) {
					// 8b
					yScale = filmAspectRatio / deviceAspectRatio;
				} else {
					// 8d
					xScale = deviceAspectRatio / filmAspectRatio;
				}
				break;
			}

			right *= xScale;
			top *= yScale;
			left = -right;
			bottom = -top;
		}
		
		else if(cameraMode == gameModeCamera) {
			deviceAspectRatio = imageWidth / (float) imageHeight;
			right = (float) Math.tan(Math.toRadians(fovX/2f))*this.nearClippingPlane;
			
			if(deviceAspectRatio >= 1) {
				top = right;
				right = top * deviceAspectRatio;
			}
			else {
				top = right * (1/deviceAspectRatio);
			}
			left = -right;
			bottom = -top;
			fovY = (float) (2*Math.atan(((top - bottom)*0.5)/this.nearClippingPlane));
			canvasWidth = right*2;
			canvasHeight = top*2;
		}
		
		if(RenderingEngine.renderingMode == RenderingEngine.PERSPECTIVE) {
			buildPerspectiveProjectionMatrix();
		}
		else if(RenderingEngine.renderingMode == RenderingEngine.ORTHO) {
			buildOrthographicProjectionMatrix();
		}
	}
	
	public void lookAtModel(Model m) {
		Vector min = m.getMin().mul(m.getScale());
		Vector max = m.getMax().mul(m.getScale());
		Vector diff = max.sub(min);
		
		float[] midFrontData = new float[3];
		midFrontData[0] = min.getDataElement(0) + diff.getDataElement(0)/2;
		midFrontData[1] = min.getDataElement(1) + diff.getDataElement(1)/2;
		midFrontData[2] = min.getDataElement(2);
		Vector midFront = new Vector(midFrontData);
		
		float[] fromData = new float[3];
		float z = 0;
		
		if(diff.getDataElement(0) > diff.getDataElement(1)) {
			z = (float) ((diff.getDataElement(0)/2f) / Math.tan(fovX));
		}
		else {
			z = (float) ((diff.getDataElement(1)/2f) / Math.tan(fovY));
		}
		fromData[0] = midFrontData[0];
		fromData[1] = midFrontData[1];
		fromData[2] = min.getDataElement(2) + z;
		Vector from = new Vector(fromData);
		
		Matrix mat = lookAtPoint(from,midFront);
		this.setCamMatrix(mat);
	}
	
	public Matrix lookAtPoint(Vector from, Vector to) {
		
		Vector dir = from.sub(to);
		Vector z = dir.normalise();
		Vector temp = new Vector(new float[] {0,1,0});
		Vector x = temp.normalise().cross(z);
		Vector y = z.cross(x);
		
		float[][] res = new float[4][4];
		
		res[0][0] = x.getDataElement(0);
		res[1][0] = x.getDataElement(1); 
		res[2][0] = x.getDataElement(2); 
		
		res[0][1] = y.getDataElement(0);
		res[1][1] = y.getDataElement(1); 
		res[2][1] = y.getDataElement(2);
		
		res[0][2] = z.getDataElement(0);
		res[1][2] = z.getDataElement(1); 
		res[2][2] = z.getDataElement(2); 
		
		res[0][3] = from.getDataElement(0);
		res[1][3] = from.getDataElement(1); 
		res[2][3] = from.getDataElement(2); 
		
		res[3][0] = 0;
		res[3][1] = 0;
		res[3][2] = 0;
		res[3][3] = 1;
		
		return new Matrix(res);
	}
	
	public void buildPerspectiveProjectionMatrix() {
		float n = this.getNearClippingPlane();
		float r = this.getRight();
		float l = this.getLeft();
		float t = this.getTop();
		float b = this.getBottom();
		float f = this.getFarClippingPlane();
		
		float[][] data = new float[][] {
			{(2*n)/(r-l),  0,        (r+l)/(r-l)  ,      0},
			{0     ,     (2*n)/(t-b),(t+b)/(t-b)  ,      0},
			{0     ,       0    ,   -(f+n)/(f-n)  , -(2*f*n)/(f-n)},
			{0     ,       0         ,    -1      ,      0}
		};
		this.perspectiveProjectionMatrix = new Matrix(data);
	}
	
	public void buildOrthographicProjectionMatrix() {
		float n = this.getNearClippingPlane();
		float r = this.getRight();
		float l = this.getLeft();
		float t = this.getTop();
		float b = this.getBottom();
		float f = this.getFarClippingPlane();
		
		float[][] data = new float[][] {
			{(2)/(r-l),        0      ,       0       , -(r+l)/(r-l)},
			{0        ,     (2)/(t-b) ,       0       , -(t+b)/(t-b)},
			{0        ,        0      ,   -(2)/(f-n)  , -(f+n)/(f-n)},
			{0        ,        0      ,       0       ,      1}
		};
		this.orthographicProjectionMatrix = new Matrix(data);
	}

	public Matrix getPerspectiveProjectionMatrix() {
		return perspectiveProjectionMatrix;
	}
	
	public Matrix getOrthographicProjectionMatrix() {
		return orthographicProjectionMatrix;
	}

	public int getCameraMode() {
		return cameraMode;
	}

	public void setCameraMode(int cameraMode) {
		this.cameraMode = cameraMode;
	}

	public int getFitMode() {
		return fitMode;
	}

	public void setFitMode(int fitMode) {
		this.fitMode = fitMode;
	}

	public int getFILL() {
		return FILL;
	}

	public int getOVERSCAN() {
		return OVERSCAN;
	}

	public float getFilmAspectRatio() {
		return filmAspectRatio;
	}

	public float getDeviceAspectRatio() {
		return deviceAspectRatio;
	}

	public float getxScale() {
		return xScale;
	}

	public float getyScale() {
		return yScale;
	}

	public int getImageWidth() {
		return imageWidth;
	}

	public void setImageWidth(int imageWidth) {
		this.imageWidth = imageWidth;
	}

	public int getImageHeight() {
		return imageHeight;
	}

	public void setImageHeight(int imageHeight) {
		this.imageHeight = imageHeight;
	}

	public float getFovX() {
		return fovX;
	}

	public void setFovX(float fovX) {
		this.fovX = fovX;
	}

	public float getFovY() {
		return fovY;
	}

	public float getRight() {
		return right;
	}

	public float getLeft() {
		return left;
	}

	public float getTop() {
		return top;
	}

	public float getBottom() {
		return bottom;
	}

	public float getFilmApertureWidth() {
		return filmApertureWidth;
	}

	public void setFilmApertureWidth(float filmApertureWidth) {
		this.filmApertureWidth = filmApertureWidth;
	}

	public float getFilmApertureHeight() {
		return filmApertureHeight;
	}

	public void setFilmApertureHeight(float filmApertureHeight) {
		this.filmApertureHeight = filmApertureHeight;
	}

	public float getFocalLength() {
		return focalLength;
	}

	public void setFocalLength(float focalLength) {
		this.focalLength = focalLength;
	}

	public float getNearClippingPlane() {
		return nearClippingPlane;
	}

	public void setNearClippingPlane(float nearClippingPlane) {
		this.nearClippingPlane = nearClippingPlane;
	}

	public float getFarClippingPlane() {
		return farClippingPlane;
	}

	public void setFarClippingPlane(float farClippingPlane) {
		this.farClippingPlane = farClippingPlane;
	}

	public float getCanvasWidth() {
		return canvasWidth;
	}

	public float getCanvasHeight() {
		return canvasHeight;
	}

	public void computeInverse() {
		camMatrixInverse = camMatrix.getInverse();
	}

	public Matrix getCamMatrix() {
		return camMatrix;
	}

	public void setCamMatrix(Matrix camMat) {
		this.camMatrix = camMat;
		computeInverse();
	}

	public Matrix getCamInverse() {
		return camMatrixInverse;
	}
}
