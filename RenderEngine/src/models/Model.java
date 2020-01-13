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
	
	protected List<Vector> rotation;
	protected Vector[] vertices;
	protected List<int[]> faces;
//	protected Matrix scaledVertices;
	protected Vector scale;
	protected Vector pos;
	protected Tick tickObj;
	protected Vector min;
	protected Vector max;
	protected Quaternion orientation;
	protected Vector[] transformedVertices;
	protected boolean isChanged = true;

	public Model(Vector[] vertices, List<int[]> faces) {
		this.faces = faces;
		this.vertices = vertices;
		rotation = new ArrayList<Vector>();
		scale = new Vector(new float[] {1,1,1});
		pos = new Vector(3,0);
		tickObj = null;
		orientation = new Quaternion(new Vector(new float[] {1,0,0,0}));
		calculateMinMax();
	}

	public void tick() {
		if(tickObj!= null) {
			tickObj.tick(this);
		}
	}
	
	public Matrix getObjectToWorldMatrix() {
		Matrix rotScalMatrix = null;
		Matrix rotationMatrix = this.orientation.getRotationMatrix();
		Matrix scalingMatrix = Matrix.getDiagonalMatrix(this.getScale());
		rotScalMatrix = rotationMatrix.matMul(scalingMatrix);
		
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
		isChanged = true;
	}

	public Vector getScale() {
		return scale;
	}

	public void setScale(Vector scale) {
		this.scale = scale;
		isChanged = true;
	}

	public Quaternion getOrientation() {
		return orientation;
	}

	public void setOrientation(Quaternion quaternion) {
		this.orientation = quaternion;
		isChanged = true;
	}

	public Vector[] getVertices() {
		return vertices;
	}

	public List<int[]> getFaces() {
		return faces;
	}
	
	public Vector[] getTranformedVertices() {
		return transformedVertices;
	}
	
	public void setTransformedVertices(Vector[] vertices) {
		this.transformedVertices = vertices;
	}

	public boolean isChanged() {
		return isChanged;
	}

	public void setChanged(boolean isChanged) {
		this.isChanged = isChanged;
	}
	
}
