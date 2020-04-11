package engine.model;

import engine.Math.Matrix;
import engine.Math.Quaternion;
import engine.Math.Vector;
import engine.DataStructure.Mesh.Mesh;

public class Model {

	public interface MiniBehaviour {
		void tick(Model m,ModelTickInput params);
	}

	public static class ModelTickInput {
		public float timeDelta;
	}

	protected Vector scale;
	protected Vector pos;
	protected MiniBehaviour miniBehaviourObj;
	protected Quaternion orientation;
	protected Matrix transformedVertices;
	protected boolean isChanged = true;

	public String identifier;

	public Mesh mesh;

	public Model(Mesh mesh,String identifier) {
		this.mesh = mesh;
		scale = new Vector(new float[] { 1, 1, 1 });
		pos = new Vector(3, 0);
		miniBehaviourObj = null;
		orientation = new Quaternion(new Vector(new float[] { 1, 0, 0, 0 }));
		this.identifier = identifier;
	}

	public void tick(ModelTickInput params) {
		if (miniBehaviourObj != null) {
			miniBehaviourObj.tick(this,params);
		}
	}

	public Matrix getObjectToWorldMatrix() {

		Matrix rotationMatrix = this.orientation.getRotationMatrix();
		Matrix scalingMatrix = Matrix.getDiagonalMatrix(this.getScale());
		Matrix rotScalMatrix = rotationMatrix.matMul(scalingMatrix);

		Matrix transformationMatrix = rotScalMatrix.addColumn(this.pos);
		transformationMatrix = transformationMatrix.addRow(new Vector(new float[] { 0, 0, 0, 1 }));
		return transformationMatrix;
	}

	public void displayMeshInformation() {
		mesh.displayMeshInformation();
	}

	public void triangulate() {
		this.mesh = ModelBuilder.triangulate(mesh,false);
	}

	public void triangulate(boolean forceUseEarClipping) {
		mesh = ModelBuilder.triangulate(mesh,forceUseEarClipping);
	}

	public Mesh getMesh() {return mesh;}

	public MiniBehaviour getMiniBehaviourObj() {
		return miniBehaviourObj;
	}

	public void setMiniBehaviourObj(MiniBehaviour miniBehaviourObj) {
		this.miniBehaviourObj = miniBehaviourObj;
	}

	public Vector getPos() {
		return pos;
	}

	public void setPos(Vector pos) {
		this.pos = pos;
		isChanged = true;
	}

	public void setPos(float x, float y, float z) {
		Vector pos = new Vector(new float[]{x,y,z});
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

	public void setScale(float v) {
		Vector scale = new Vector(new float[]{v,v,v});
		this.scale = scale;
		isChanged = true;
	}

	public void setScale(float x, float y, float z) {
		Vector scale = new Vector(new float[]{x,y,z});
		this.scale = scale;
		isChanged = true;
	}

	public Vector getCentre() {
		Vector res = new Vector(new float[]{0,0,0});
		for(Vector v: mesh.getVertices()) {
			res = res.add(v.removeDimensionFromVec(3).mul(scale));
		}
		res = res.scalarMul(1f/mesh.getVertices().size());
		res = res.add(pos);

		return res;
	}

	public Quaternion getOrientation() {
		return orientation;
	}

	public void setOrientation(Quaternion quaternion) {
		this.orientation = quaternion;
		isChanged = true;
	}

	public Matrix getTranformedVertices() {
		return transformedVertices;
	}

	public void setTransformedVertices(Matrix vertices) {
		this.transformedVertices = vertices;
	}

	public boolean isChanged() {
		return isChanged;
	}

	public void setChanged(boolean isChanged) {
		this.isChanged = isChanged;
	}

}
