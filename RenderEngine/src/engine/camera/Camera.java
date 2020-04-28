package engine.camera;

import engine.Math.Matrix;
import engine.Math.Quaternion;
import engine.Math.Vector;
import engine.game.Game;
import engine.utils.Utils;
import engine.model.Model;
import engine.renderingEngine.RenderingEngine.ProjectionMode;

public class Camera {

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

	private final float inchToMm = 25.4f;
	
	private boolean shouldUpdateValues = false;
	
	private Quaternion orientation;

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

	public Camera(Game game, Quaternion quaternion, Quaternion forward, Quaternion up, Vector pos, float fovX, float nearClippingPlane, float farClippingPlane,
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
		this.pos = pos;
		canvasWidth = 0;
		canvasHeight = 0;
		this.orientation = quaternion;
		
		if(quaternion == null) {
			this.setOrientation(new Quaternion(new Vector(new float[] {1,0, 0, 0})));
		}
		
		if(pos == null) {
			this.pos = new Vector(new float[] {0,0,0});
		}
		
		updateValues();
	}

	public void tick() {
		
		if(shouldUpdateValues) {
			updateValues();
			this.setShouldUpdateValues(false);
		}
	}

	public void updateValues() {
		 if (cameraMode == gameModeCamera) {
			
			 imageAspectRatio = imageWidth / (float) imageHeight;

			if (game.getRenderingEngine().getProjectionMode() == ProjectionMode.PERSPECTIVE) {
				
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
				buildOrthographicProjectionMatrix();
			}
			else if(game.getRenderingEngine().getProjectionMode() == ProjectionMode.ORTHO) {
				
				Vector[] bounds = Utils.getWorldBoundingBox(game.getModels());
				Vector minCam = (getWorldToCam().matMul(bounds[0].addDimensionToVec(1))).toVector();
				Vector maxCam = (getWorldToCam().matMul(bounds[1].addDimensionToVec(1))).toVector();
//				maxCam.display();
				float maxX = Math.max(Math.abs(minCam.get(0)), Math.abs(maxCam.get(0)));
				float maxY = Math.max(Math.abs(minCam.get(1)), Math.abs(maxCam.get(1)));
				float max = Math.max(maxX, maxY);
//				System.out.println(max);
				right = max * imageAspectRatio;
				top = max; 
			    left = -right;
			    bottom = -top;
			    fovY = (float) (2 * Math.atan(((top - bottom) * 0.5) / this.nearClippingPlane));
				canvasWidth = right * 2;
				canvasHeight = top * 2;

				buildPerspectiveProjectionMatrix();
				buildOrthographicProjectionMatrix();
			}
		}
	}

	public Vector lookAtModel(Model m) {

		float[] dataMin = new float[3];
		dataMin[0] = Float.POSITIVE_INFINITY;
		dataMin[1] = Float.POSITIVE_INFINITY;
		dataMin[2] = Float.POSITIVE_INFINITY;

		float[] dataMax = new float[3];
		dataMax[0] = Float.NEGATIVE_INFINITY;
		dataMax[1] = Float.NEGATIVE_INFINITY;
		dataMax[2] = Float.NEGATIVE_INFINITY;

		for (Vector v : m.getMesh().getVertices()) {
			if (v.get(0) < dataMin[0]) {
				dataMin[0] = v.get(0);
			}
			if (v.get(1) < dataMin[1]) {
				dataMin[1] = v.get(1);
			}
			if (v.get(2) < dataMin[2]) {
				dataMin[2] = v.get(2);
			}

			if (v.get(0) > dataMax[0]) {
				dataMax[0] = v.get(0);
			}
			if (v.get(1) > dataMax[1]) {
				dataMax[1] = v.get(1);
			}
			if (v.get(2) > dataMax[2]) {
				dataMax[2] = v.get(2);
			}
		}

		Vector minT = new Vector(dataMin);
		Vector maxT = new Vector(dataMax);
		
		Vector min = ((minT.mul(m.getScale())).add(m.getPos()));
		Vector max = (maxT.mul(m.getScale()).add(m.getPos()));
		Vector diff = max.sub(min);
		
		float[] midFrontData = new float[3];
		midFrontData[0] = min.get(0) + diff.get(0) / 2;
		midFrontData[1] = min.get(1) + diff.get(1) / 2;
		midFrontData[2] = min.get(2);
		Vector to = new Vector(midFrontData);

		float[] fromData = new float[3];
		float z = 0;

		if (diff.get(0) > diff.get(1)) {
			z = (float) ((diff.get(0) / 2f) / Math.tan(fovX / 2f));
		} else {
			z = (float) ((diff.get(1) / 2f) / Math.tan(fovY / 2f));
		}

		fromData[0] = midFrontData[0];
		fromData[1] = midFrontData[1];
		fromData[2] = (max.get(2) + z);
		
		Vector from = new Vector(fromData);
		return lookAtPoint(from, to);
	}

	public Vector lookAtPoint(Vector from, Vector to) {
		
		Vector dir = (from.sub(to)).normalise();
		float yawIncrease   = (float) Math.toRadians(Math.acos(dir.dot(new Vector(new float[] {1,0,0}))));
		float pitchIncrease = (float) Math.toRadians(Math.acos(dir.dot(new Vector(new float[] {0,0,1}))));
		
		Quaternion pitch = Quaternion.getAxisAsQuat(new Vector(new float[] {1,0,0}),pitchIncrease);
		Quaternion yaw = Quaternion.getAxisAsQuat(new Vector(new float[] {0,1,0}),yawIncrease);
		
		Quaternion q = new Quaternion(new Vector(new float[] {1,0,0,0}));
		q = q.multiply(pitch);
		q = yaw.multiply(q);
		this.setOrientation(q);
		
		this.setPos(from);
		return from;
	}
	
	public Vector[] getAxesFromforwardVec(Vector v) {
		
		Vector temp = new Vector(new float[] { 0, 1, 0 });
		Vector x = null;
		Vector y = null;
		Vector z = null;
		
		if(temp.sub(v).getNorm() != 0) {
		z = v.normalise();
		temp = temp.normalise();
		x = temp.cross(z);
		y = z.cross(x);
		}
		else {
			z = temp;
			x = new Vector(new float[] {-1,0,0});
			y = new Vector(new float[] {0,0,-1});
		}
		
		System.out.println(y.dot(z));
		
		Vector[] res = new Vector[3];
		res[0] = x;
		res[1] = y;
		res[2] = z;
		
		return res;
	}

	public void buildPerspectiveProjectionMatrix() {
		float n = this.getNearClippingPlane();
		float r = this.getRight();
		float l = this.getLeft();
		float t = this.getTop();
		float b = this.getBottom();
		float f = this.getFarClippingPlane();

		float[][] data = new float[][] {{ (2 * n) / (r - l), 0,				   (r + l) / (r - l),   0 },
										{ 0, 				(2 * n) / (t - b), (t + b) / (t - b),   0 }, 
										{ 0,				 0, 			   -(f + n) / (f - n), -(2 * f * n) / (f - n) },
										{ 0, 				 0, 			   -1,                  0 } };
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

	public Quaternion getOrientation() {
		return orientation;
	}

	public void setOrientation(Quaternion orientation) {
		this.orientation = orientation;
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

	public Matrix getCamToWorld() {
		Matrix m = orientation.getRotationMatrix();
		m = m.addColumn(pos);
		m = m.addRow(new Vector(new float[] {0,0,0,1}));
		return m;
	}

	public Matrix getWorldToCam() {
		Matrix m_ = orientation.getInverse().getRotationMatrix();
		Vector pos_ = (m_.matMul(pos).toVector()).scalarMul(-1);
		Matrix res = m_.addColumn(pos_);
		res = res.addRow(new Vector(new float[]{0,0,0,1}));
		return res;
	}

	public Vector getPos() {
		return pos;
	}

	public void setPos(Vector pos) {
		this.pos = pos;
	}
}
