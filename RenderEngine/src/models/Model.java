package models;

import java.util.ArrayList;
import java.util.List;

import Math.Matrix;
import Math.Quaternion;
import Math.Vector;

public class Model {
	
	public interface Tick {
		public void tick(Model m);
	}
	
	private List<Vector> rotation;
	private Vector[] vertices;
	private Vector[] connections;
//	private Matrix scaledVertices;
	private Vector scale;
	private Vector pos;
	private Tick tickObj;
	private Vector min;
	private Vector max;
	private Quaternion quaternion;

	public Model(Vector[] vertices, Vector[] connections) {
		this.connections = connections;
		this.vertices = vertices;
		rotation = new ArrayList<Vector>();
		scale = new Vector(new float[] {1,1,1});
		pos = new Vector(3,0);
		tickObj = null;
		quaternion = new Quaternion(new Vector(new float[] {0,1,0}),0);
		calculateMinMax();
	}

	public void tick() {
		if(tickObj!= null) {
			tickObj.tick(this);
		}
	}
	
	public Matrix getObjectToWorldMatrix() {

		Matrix rotScalMatrix = null;
//		Matrix rotationMatrix = Matrix.getIdentityMatrix(3);
//
//		for (Vector v : this.getRotation()) {
//
//			switch ((int) v.getDataElement(0)) {
//			case 0:
//				rotationMatrix = Matrix.getRotateX(v.getDataElement(1)).matMul(rotationMatrix);
//				break;
//			case 1:
//				rotationMatrix = Matrix.getRotateY(v.getDataElement(1)).matMul(rotationMatrix);
//				break;
//			case 2:
//				rotationMatrix = Matrix.getRotateZ(v.getDataElement(1)).matMul(rotationMatrix);
//				break;
//			default:
//				System.err.println("Model rotation argument 1 should be be integers 0,1 or 2");
//				break;
//			}
//		}
		
		Matrix rotationMatrix = this.quaternion.getRotationMatrix();
		Matrix scalingMatrix = Matrix.getDiagonalMatrix(this.getScale());
		rotScalMatrix = rotationMatrix.matMul(scalingMatrix);
//
//		float[][] data = new float[4][4];
//		for (int r = 0; r < 3; r++) {
//			for (int c = 0; c < 3; c++) {
//				data[r][c] = rotScalMatrix.getData()[r][c];
//			}
//		}
//		
//		for (int r = 0; r < 4; r++) {
//			if (r != 3) {
//				data[r][3] = this.getPos().getDataElement(r);
//			} else {
//				data[r][3] = 1;
//			}
//		}
//
//		for (int c = 0; c < 3; c++) {
//			data[3][c] = 0;
//		}
//		Matrix transformationMatrix = new Matrix(data);
		
		Matrix transformationMatrix = rotScalMatrix.addColumn(this.pos);
		transformationMatrix = transformationMatrix.addRow(new Vector(new float[]{0,0,0,1}));
		return transformationMatrix;
	}
	
	public void calculateMinMax() {
		
		float[] dataMin = new float[3];
		dataMin[0] = Float.POSITIVE_INFINITY;
		dataMin[1] = Float.POSITIVE_INFINITY;
		dataMin[2] = Float.POSITIVE_INFINITY;
		
		float[] dataMax = new float[3];
		dataMax[0] = Float.NEGATIVE_INFINITY;
		dataMax[1] = Float.NEGATIVE_INFINITY;
		dataMax[2] = Float.NEGATIVE_INFINITY;
		
		for(Vector v : vertices) {
			if(v.getDataElement(0) < dataMin[0]) {
				dataMin[0] = v.getDataElement(0);
			}
			if(v.getDataElement(1) < dataMin[1]) {
				dataMin[1] = v.getDataElement(1);
			}
			if(v.getDataElement(2) < dataMin[2]) {
				dataMin[2] = v.getDataElement(2);
			}
			
			if(v.getDataElement(0) > dataMax[0]) {
				dataMax[0] = v.getDataElement(0);
			}
			if(v.getDataElement(1) > dataMax[1]) {
				dataMax[1] = v.getDataElement(1);
			}
			if(v.getDataElement(2) > dataMax[2]) {
				dataMax[2] = v.getDataElement(2);
			}
		}
		
		min = new Vector(dataMin);
		max = new Vector(dataMax);
		
	}

	public Tick getTickObj() {
		return tickObj;
	}

	public void setTickObj(Tick tickObj) {
		this.tickObj = tickObj;
	}

	public Vector getPos() {
		return pos;
	}

	public Vector getMin() {
		return min;
	}

	public Vector getMax() {
		return max;
	}

	public void setPos(Vector pos) {
		this.pos = pos;
	}

	public Vector getScale() {
		return scale;
	}

	public void setScale(Vector scale) {
		this.scale = scale;
//		scaledVertices = new Matrix(vertices).scalarMul(scale);
	}

	public List<Vector> getRotation() {
		return rotation;
	}

	public void setRotation(List<Vector> rotation) {
		
		for(Vector v: rotation) {
			if(v.getDataElement(1) > 360) {
				v.setDataElement(1, v.getDataElement(1) - 360);
			}
			
			if(v.getDataElement(1) < -360) {
				v.setDataElement(1, v.getDataElement(1) + 360);
			}
		}
		
		this.rotation = rotation;
		
	}

	public Quaternion getQuaternion() {
		return quaternion;
	}

	public void setQuaternion(Quaternion quaternion) {
		this.quaternion = quaternion;
	}

	public Vector[] getVertices() {
		return vertices;
	}

	public Vector[] getConnections() {
		return connections;
	}
	
	public void setVerticesConnections(Vector[] vertices, Vector[] connections) {
		this.connections = connections;
		this.vertices = vertices;
//		scaledVertices = new Matrix(vertices).scalarMul(scale);
	}
	
}
