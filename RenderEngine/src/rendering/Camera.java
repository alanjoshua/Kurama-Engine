package rendering;

import java.awt.Canvas;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

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
	private float imageAspectRatio = 1;
	private float xScale = 1;
	private float yScale = 1;
	
	private int cameraMode = gameModeCamera;
	private static final int simulateTrueCamera = 1;
	public static final int gameModeCamera = 0;

	private Matrix perspectiveProjectionMatrix = null;
	private Matrix orthographicProjectionMatrix = null;
	
	private Vector pos = null;
	private Vector pointingDir;
	private float[][] data;

	private final float inchToMm = 25.4f;
	
	private boolean shouldUpdateValues = false;
	private boolean shouldUpdateCameraMatrix = false;
	
	private Vector mouse = null;

//	public Camera(Game game, float[][] data, float focalLength, float filmApertureWidth, float filmApertureHeight,
//			float nearClippingPlane, float farClippingPlane, int imageWidht, int imageHeight, int cameraMode) {
//		this.camMatrix = new Matrix(data);
//		this.game = game;
//		this.filmApertureWidth = filmApertureWidth;
//		this.filmApertureHeight = filmApertureHeight;
//		this.focalLength = focalLength;
//		this.nearClippingPlane = nearClippingPlane;
//		this.farClippingPlane = farClippingPlane;
//		this.imageWidth = imageWidht;
//		this.imageHeight = imageHeight;
//		this.cameraMode = cameraMode;
//
//		canvasWidth = 0;
//		canvasHeight = 0;
//
//		computeInverse();
//		updateValues();
//	}

	public Camera(Game game, float[][] data, Vector pos, float fovX, float nearClippingPlane, float farClippingPlane,
			int imageWidht, int imageHeight) {
		this.game = game;
		this.filmApertureWidth = 0;
		this.filmApertureHeight = 0;
		this.focalLength = 0;
		this.nearClippingPlane = nearClippingPlane;
		this.farClippingPlane = farClippingPlane;
		this.imageWidth = imageWidht;
		this.imageHeight = imageHeight;
		this.fovX = fovX;
		this.data = data;
		this.pos = pos;
		canvasWidth = 0;
		canvasHeight = 0;
		
		if(pos == null) {
			this.pos = new Vector(new float[] {0,0,0});
		}
		
		this.setCamMatrix(this.makeCameraMatrix());
		updateValues();
	}

	public void tick() {
		
		if(shouldUpdateCameraMatrix) {
			this.setCamMatrix(this.makeCameraMatrix());
			this.setShouldUpdateCameraMatrix(false);
			
			if(RenderingEngine.renderingMode == RenderingEngine.ORTHO) {
				this.setShouldUpdateValues(true);
			}
		}
		
		if(shouldUpdateValues) {
			updateValues();
			this.setShouldUpdateValues(false);
		}
	}

	public void updateValues() {
		
		 if (cameraMode == gameModeCamera) {
			
			 imageAspectRatio = imageWidth / (float) imageHeight;

			if (RenderingEngine.renderingMode == RenderingEngine.PERSPECTIVE) {
				
				right = (float) Math.tan(Math.toRadians(fovX / 2f)) * this.nearClippingPlane;

				if (imageAspectRatio >= 1) {
					top = right;
					right = top * imageAspectRatio;
				} else {
					top = right * (1 / imageAspectRatio);
				}
				left = -right;
				bottom = -top;
				fovY = (float) (2 * Math.atan(((top - bottom) * 0.5) / this.nearClippingPlane));
				canvasWidth = right * 2;
				canvasHeight = top * 2;
				
				buildPerspectiveProjectionMatrix();
			}
			else if(RenderingEngine.renderingMode == RenderingEngine.ORTHO) {
				
				Vector[] bounds = getWorldBoundingBox();
				Vector minCam = (camMatrixInverse.matMul(bounds[0].addDimensionToVec(1))).toVector();
				Vector maxCam = (camMatrixInverse.matMul(bounds[1].addDimensionToVec(1))).toVector();
//				maxCam.display();
				float maxX = Math.max(Math.abs(minCam.getDataElement(0)), Math.abs(maxCam.getDataElement(0)));
				float maxY = Math.max(Math.abs(minCam.getDataElement(1)), Math.abs(maxCam.getDataElement(1)));
				float max = Math.max(maxX, maxY);
//				System.out.println(max);
				right = max * imageAspectRatio;
				top = max; 
			    left = -right;
			    bottom = -top;
			    fovY = (float) (2 * Math.atan(((top - bottom) * 0.5) / this.nearClippingPlane));
				canvasWidth = right * 2;
				canvasHeight = top * 2;
			    
				buildOrthographicProjectionMatrix();
			}
		}
	}
	
	public Matrix makeCameraMatrix() {
		
		Matrix temp = new Matrix(this.getData());
		temp = temp.addColumn(getPos());
		getPos().display();
		temp = temp.addRow(new Vector(new float[] {0,0,0,1}));

		return temp;
	}
	
	public void mouseScrollInput(MouseWheelEvent e) {
		float scrollDir = (float) e.getPreciseWheelRotation();
		
		Vector v = null;
		
		if(scrollDir < 0) {    
			v = pos.sub(pointingDir.scalarMul(1f));
		}
		else {
			v = pos.add(pointingDir.scalarMul(1f));
		}
		
		this.setPos(v);
		this.setShouldUpdateCameraMatrix(true);
	}
	
	public void mouseDragInput(MouseEvent e) {
		
	}
	
	public void mouseMoveInput(MouseEvent e) {
		this.mouse = new Vector(new float[]{e.getX(), (imageHeight - e.getY())});
		
		float[] canvasCoords = new float[3];
		canvasCoords[0] = (((mouse.getDataElement(0) / imageWidth) * 2) - 1)  * right;
		canvasCoords[1] = (((mouse.getDataElement(1) / imageHeight) * 2) - 1) * top;
		canvasCoords[2]  = this.nearClippingPlane;
		
		Vector canvasCoordsVec = new Vector(canvasCoords);
		this.setDir(canvasCoordsVec.normalise());
		
		Vector[] axes = this.getAxesFromforwardVec(this.pointingDir.scalarMul(-1f));
		
		float[][] data = new float[3][3];
		
		for(int i = 0 ;i < 3;i++) {
			for(int j = 0;j < 3;j++) {
				data[i][j] = axes[i].getDataElement(j);
			}
		}

		this.data = data;
		this.setShouldUpdateCameraMatrix(true);
	}

	public void lookAtModel(Model m) {
		Vector min = ((m.getMin().mul(m.getScale())).add(m.getPos()));
		Vector max = (m.getMax().mul(m.getScale()).add(m.getPos()));
		Vector diff = max.sub(min);
		
		float[] midFrontData = new float[3];
		midFrontData[0] = min.getDataElement(0) + diff.getDataElement(0) / 2;
		midFrontData[1] = min.getDataElement(1) + diff.getDataElement(1) / 2;
		midFrontData[2] = min.getDataElement(2);
		Vector midFront = new Vector(midFrontData);

		float[] fromData = new float[3];
		float z = 0;

		if (diff.getDataElement(0) > diff.getDataElement(1)) {
			z = (float) ((diff.getDataElement(0) / 2f) / Math.tan(fovX / 2f));
		} else {
			z = (float) ((diff.getDataElement(1) / 2f) / Math.tan(fovY / 2f));
		}

		fromData[0] = midFrontData[0];
		fromData[1] = midFrontData[1] + 5;
		fromData[2] = min.getDataElement(2) - z;
		Vector from = new Vector(fromData);
		Matrix mat = lookAtPoint(from, midFront);
		
		this.pos = from;
		this.data = Matrix.getSmallerMatrix(mat, 3, 3).getData();
		this.setShouldUpdateCameraMatrix(true);
	}

	public Matrix lookAtPoint(Vector from, Vector to) {

		Vector dir = from.sub(to);
		Vector z = dir.normalise();
		Vector[] axes = this.getAxesFromforwardVec(z);
		Vector x = axes[0];
		Vector y = axes[1];
		this.setDir(dir.scalarMul(-1));
		
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
	
	public Vector[] getAxesFromforwardVec(Vector v) {
		Vector z = v.normalise();
		Vector temp = new Vector(new float[] { 0, 1, 0 });
		temp = temp.normalise();
		Vector x = temp.cross(z);
		Vector y = z.cross(x);
		
		Vector[] res = new Vector[3];
		res[0] = x;
		res[1] = y;
		res[2] = z;
		
		return res;
	}

	public Vector[] getWorldBoundingBox() {

		float[] dataMin = new float[3];
		dataMin[0] = Float.POSITIVE_INFINITY;
		dataMin[1] = Float.POSITIVE_INFINITY;
		dataMin[2] = Float.POSITIVE_INFINITY;

		float[] dataMax = new float[3];
		dataMax[0] = Float.NEGATIVE_INFINITY;
		dataMax[1] = Float.NEGATIVE_INFINITY;
		dataMax[2] = Float.NEGATIVE_INFINITY;

		for (Model m : game.getModels()) {
			for (Vector vv : m.getVertices()) {
				Vector v = (vv.mul(m.getScale())).add(m.getPos());
				if (v.getDataElement(0) < dataMin[0]) {
					dataMin[0] = v.getDataElement(0);
				}
				if (v.getDataElement(1) < dataMin[1]) {
					dataMin[1] = v.getDataElement(1);
				}
				if (v.getDataElement(2) < dataMin[2]) {
					dataMin[2] = v.getDataElement(2);
				}

				if (v.getDataElement(0) > dataMax[0]) {
					dataMax[0] = v.getDataElement(0);
				}
				if (v.getDataElement(1) > dataMax[1]) {
					dataMax[1] = v.getDataElement(1);
				}
				if (v.getDataElement(2) > dataMax[2]) {
					dataMax[2] = v.getDataElement(2);
				}
			}
		}
		Vector min = new Vector(dataMin);
		Vector max = new Vector(dataMax);
		Vector[] res = new Vector[2];
		res[0] = min;
		res[1] = max;
		return res;
	}

	public void buildPerspectiveProjectionMatrix() {
		float n = this.getNearClippingPlane();
		float r = this.getRight();
		float l = this.getLeft();
		float t = this.getTop();
		float b = this.getBottom();
		float f = this.getFarClippingPlane();

		float[][] data = new float[][] { { (2 * n) / (r - l), 0, (r + l) / (r - l), 0 },
				{ 0, (2 * n) / (t - b), (t + b) / (t - b), 0 }, { 0, 0, -(f + n) / (f - n), -(2 * f * n) / (f - n) },
				{ 0, 0, -1, 0 } };
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
				{ (2) / (r - l), 0, 0, -(r + l) / (r - l) },
				{ 0, (2) / (t - b), 0, -(t + b) / (t - b) }, 
				{ 0, 0, -(2) / (f - n), -(f + n) / (f - n) },
				{ 0, 0, 0, 1 } };
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
		return imageAspectRatio;
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

	public boolean isShouldUpdateValues() {
		return shouldUpdateValues;
	}

	public void setShouldUpdateValues(boolean shouldUpdateValues) {
		this.shouldUpdateValues = shouldUpdateValues;
	}

	public boolean isShouldUpdateCameraMatrix() {
		return shouldUpdateCameraMatrix;
	}

	public void setShouldUpdateCameraMatrix(boolean shouldUpdateCameraMatrix) {
		this.shouldUpdateCameraMatrix = shouldUpdateCameraMatrix;
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

	public float[][] getData() {
		return data;
	}

	public void setData(float[][] data) {
		this.data = data;
	}

	public Vector getMouse() {
		return mouse;
	}

	public void setMouse(Vector mouse) {
		this.mouse = mouse;
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

	public Vector getDir() {
		return pointingDir;
	}

	public void setDir(Vector dir) {
		this.pointingDir = dir;
	}

	public Matrix getCamMatrix() {
		return camMatrix;
	}

	public void setCamMatrix(Matrix camMat) {
		this.camMatrix = camMat;
		this.computeInverse();
	}

	public Matrix getCamInverse() {
		return camMatrixInverse;
	}

	public Vector getPos() {
		return pos;
	}

	public void setPos(Vector pos) {
		this.pos = pos;
	}
}
