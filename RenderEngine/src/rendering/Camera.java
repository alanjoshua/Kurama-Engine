package rendering;

import java.awt.event.MouseWheelEvent;

import Math.Matrix;
import Math.Quaternion;
import Math.Vector;
import main.Game;
import models.Model;

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
	
	private Quaternion quaternion;

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

	public Camera(Game game, Quaternion quaternion, Vector pos, float fovX, float nearClippingPlane, float farClippingPlane,
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
		
//		this.forward = forward;
//		this.up = up;
		this.quaternion = quaternion;
		
//		if(forward == null) {
//			this.forward = new Quaternion(new Vector(new float[] {0,0,1}));
//		}
//		
//		if(up == null) {
//			this.up = new Quaternion(new Vector(new float[] {0,1,0}));
//		}
		
		if(quaternion == null) {
//			this.quaternion = new Quaternion(new Vector(new float[] {1,0,0}),0);
			this.setQuaternion(new Quaternion(new Vector(new float[] {0, 0, 0}),0));
		}
		
		if(pos == null) {
			this.pos = new Vector(new float[] {0,0,0});
		}
		
		updateValues();
	}

	public void tick() {
		
//		if(shouldUpdateCameraMatrix) {
//			this.setCamToWorld(this.makeCameraMatrix());
//			float[] temp = new float[3];
//			for(int i = 0;i < 3;i++) {
//				temp[i] = this.getData()[i][2];
//			}
//			this.setDirection(new Vector(temp));
//			this.setShouldUpdateCameraMatrix(false);
			
//			if(RenderingEngine.renderingMode == RenderingEngine.ORTHO) {
//				this.setShouldUpdateValues(true);
//			}
//		}
		
		if(shouldUpdateValues) {
			updateValues();
			this.setShouldUpdateValues(false);
		}
	}
	
	public void rotate(Quaternion rotation) {
		this.quaternion = rotation.multiply(this.quaternion);
//		this.forward = rotation.multiply(forward);
//		this.up = rotation.multiply(up);
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
				Vector minCam = (getWorldToCam().matMul(bounds[0].addDimensionToVec(1))).toVector();
				Vector maxCam = (getWorldToCam().matMul(bounds[1].addDimensionToVec(1))).toVector();
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
	
//	public void mouseScrollInput(MouseWheelEvent e) {
//		float scrollDir = (float) e.getPreciseWheelRotation();
//		
//		Vector v = null;
//		
//		if(scrollDir < 0) {    
//			v = pos.sub(pointingDir.scalarMul(1f));
//		}
//		else {
//			v = pos.add(pointingDir.scalarMul(1f));
//		}
//		
//		this.setPos(v);
//		this.setShouldUpdateCameraMatrix(true);
//	}
//	
//	public void mouseMoveInput() {
//		this.mouse = game.getInput().getPosition();
//		
//		float[] canvasCoords = new float[3];
//		canvasCoords[0] = (((mouse.getDataElement(0) / imageWidth) * 2) - 1)  * right;
//		canvasCoords[1] = (((mouse.getDataElement(1) / imageHeight) * 2) - 1) * top;
//		canvasCoords[2]  = this.nearClippingPlane;
//		
//		Vector canvasCoordsVec = new Vector(canvasCoords);
//		this.setDirection(canvasCoordsVec.normalise());
//		
//		Vector[] axes = this.getAxesFromforwardVec(this.pointingDir.scalarMul(-1f));
//		
//		float[][] data = new float[3][3];
//		
//		for(int i = 0 ;i < 3;i++) {
//			for(int j = 0;j < 3;j++) {
//				data[i][j] = axes[i].getDataElement(j);
//			}
//		}
//
//		this.data = data;
//		this.setShouldUpdateCameraMatrix(true);
//	}

	public void lookAtModel(Model m) {
		
		Vector min = ((m.getMin().mul(m.getScale())).add(m.getPos()));
		Vector max = (m.getMax().mul(m.getScale()).add(m.getPos()));
		Vector diff = max.sub(min);
		
//		min.display();
		
		float[] midFrontData = new float[3];
		midFrontData[0] = min.getDataElement(0) + diff.getDataElement(0) / 2;
		midFrontData[1] = min.getDataElement(1) + diff.getDataElement(1) / 2;
		midFrontData[2] = min.getDataElement(2);
		Vector to = new Vector(midFrontData);

		float[] fromData = new float[3];
		float z = 0;

		if (diff.getDataElement(0) > diff.getDataElement(1)) {
			z = (float) ((diff.getDataElement(0) / 2f) / Math.tan(fovX / 2f));
		} else {
			z = (float) ((diff.getDataElement(1) / 2f) / Math.tan(fovY / 2f));
		}

		fromData[0] = midFrontData[0];
		fromData[1] = midFrontData[1];
		fromData[2] = (max.getDataElement(2) + z);
		Vector from = new Vector(fromData);
		this.setPos(from);
		
		Vector temp = from.sub(to);

//		Quaternion localRot = Quaternion.eulerToQuaternion(temp.scalarMul(1));
//		this.setQuaternion(this.getQuaternion().multiply(localRot));
	}

	public void lookAtPoint(Vector from, Vector to) {

//		Vector dir = from.sub(to);
//		Vector z = dir.normalise();
//		Vector[] axes = this.getAxesFromforwardVec(z);
//		Vector x = axes[0];
//		Vector y = axes[1];
		
		
		
//		float[][] res = new float[4][4];
//
//		res[0][0] = x.getDataElement(0);
//		res[1][0] = x.getDataElement(1);
//		res[2][0] = x.getDataElement(2);
//
//		res[0][1] = y.getDataElement(0);
//		res[1][1] = y.getDataElement(1);
//		res[2][1] = y.getDataElement(2);
//
//		res[0][2] = z.getDataElement(0);
//		res[1][2] = z.getDataElement(1);
//		res[2][2] = z.getDataElement(2);
//
//		res[0][3] = from.getDataElement(0);
//		res[1][3] = from.getDataElement(1);
//		res[2][3] = from.getDataElement(2);
//
//		res[3][0] = 0;
//		res[3][1] = 0;
//		res[3][2] = 0;
//		res[3][3] = 1;
//
//		return new Matrix(res);
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
	
//	public void renderAxes(Graphics2D g) {
//		Matrix dat = new Matrix(data);
//		dat = dat.addColumn(new Vector(new float[] {0,0,0}));
//		dat = dat.addRow(new Vector(new float[] {0,0,0,1}));
//		
////		Vector[] axes = (camMatrix.matMul(dat)).convertToVectorArray();
//		
//		Vector offset = new Vector(new float[] {imageWidth/2,imageHeight/2,0});
//		Vector[] axes = (new Matrix(data).scalarMul(-100)).convertToVectorArray();
//		
//		for(int i =0; i < axes.length;i++) {
////			axes[i].getData()[1] *= -1;;
//			axes[i] = axes[i].add(offset);
//		}
//		
//		Vector tempPos = pos.add(offset);
//		
//		g.setColor(Color.green);
//		RenderingEngine.drawLine(g, tempPos, axes[0]);
//		g.drawString("X", axes[0].getDataElement(0), axes[0].getDataElement(1));
//		
//		g.setColor(Color.blue);
//		RenderingEngine.drawLine(g, tempPos, axes[1]);
//		g.drawString("Y", axes[1].getDataElement(0), axes[1].getDataElement(1));
//		
//		g.setColor(Color.red);
//		RenderingEngine.drawLine(g, tempPos, axes[2]);
//		g.drawString("Z", axes[2].getDataElement(0), axes[2].getDataElement(1));
//	}

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

	public Quaternion getQuaternion() {
		return quaternion;
	}

	public void setQuaternion(Quaternion quaternion) {
		this.quaternion = quaternion;
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
//		Vector[] axes = new Vector[3];
//		axes[2] = forward.getPureVec();
//		axes[1] = up.getPureVec();
//		axes[0] = axes[1].cross(axes[2]);
		
		Matrix m = quaternion.getRotationMatrix();
//		Matrix m = new Matrix(axes);
		m = m.addColumn(pos);
		m = m.addRow(new Vector(new float[] {0,0,0,1}));
		return m;
	}

	public Matrix getWorldToCam() {
//		Vector[] axes = new Vector[3];
//		axes[2] = (forward.getPureVec()).scalarMul(-1);
//		axes[1] = (up.getPureVec()).scalarMul(-1);
//		axes[0] = axes[1].cross(axes[2]);
		
		Matrix m_ = quaternion.getInverse().getRotationMatrix();
//		Matrix m_ = new Matrix(axes);
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
