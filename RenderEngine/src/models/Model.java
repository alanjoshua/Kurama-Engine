package models;

import java.util.ArrayList;
import java.util.LinkedList;
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
	protected Vector[] textureCoords;
	protected Vector[] normals;
	protected List<int[]> faces;
	protected List<int[]> textureFaces;
	protected List<int[]> normalFaces;

	protected Vector scale;
	protected Vector pos;
	protected Tick tickObj;
	protected Vector min;
	protected Vector max;
	protected Quaternion orientation;
	protected Vector[] transformedVertices;
	protected boolean isChanged = true;

	public Model(Vector[] vertices, List<int[]> faces, Vector[] textureCoords, List<int[]> textureFaces,
			Vector[] normals, List<int[]> normalFaces) {
		this.vertices = vertices;
		this.faces = faces;

		this.textureFaces = textureFaces;
		this.textureCoords = textureCoords;

		this.normalFaces = normalFaces;
		this.normals = normals;

		rotation = new ArrayList<Vector>();
		scale = new Vector(new float[] { 1, 1, 1 });
		pos = new Vector(3, 0);
		tickObj = null;
		orientation = new Quaternion(new Vector(new float[] { 1, 0, 0, 0 }));
		calculateMinMax();
	}

	public void tick() {
		if (tickObj != null) {
			tickObj.tick(this);
		}
	}

	public Matrix getObjectToWorldMatrix() {
		Matrix rotScalMatrix = null;
		Matrix rotationMatrix = this.orientation.getRotationMatrix();
		Matrix scalingMatrix = Matrix.getDiagonalMatrix(this.getScale());
		rotScalMatrix = rotationMatrix.matMul(scalingMatrix);

		Matrix transformationMatrix = rotScalMatrix.addColumn(this.pos);
		transformationMatrix = transformationMatrix.addRow(new Vector(new float[] { 0, 0, 0, 1 }));
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

		for (Vector v : vertices) {
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

		min = new Vector(dataMin);
		max = new Vector(dataMax);

	}

	public void triangulate() {
		List<int[]> facesTriangle = new ArrayList<int[]>();
		List<int[]> textureFacesTriangle = new ArrayList<int[]>();
		List<int[]> normalFacesTriangle = new ArrayList<int[]>();

		if (faces != null) {

			for (int[] x : faces) {

				if (x != null && x.length > 3) {

					if (x.length == 4) {
						int[] t1 = new int[3];
						int[] t2 = new int[3];
						t1[0] = x[0];
						t1[1] = x[1];
						t1[2] = x[3];
						t2[0] = x[1];
						t2[1] = x[2];
						t2[2] = x[3];
						facesTriangle.add(t1);
						facesTriangle.add(t2);
					}
					else {
						List<Integer> reflex = new ArrayList<Integer>();
						List<Integer> convex = new ArrayList<Integer>();
						List<Integer> ears = new ArrayList<Integer>();
						List<Integer> vertsLeft = new ArrayList<Integer>();
						
						for(int i = 0;i < x.length;i++) {
							vertsLeft.add(x[i]);
						}
						
//						Calculating the internal angle of each vertex and adds them to either the reflex or concave list 
						for(int i = 0;i < x.length;i++) {
							Vector v0,vi,v2;
							if(i == 0) {
								v0  = vertices[x[x.length - 1]];
								vi = vertices[0];
								v2 = vertices[1];
							}
							else if(i == x.length - 1) {
								v0  = vertices[i - 1];
								vi = vertices[i];
								v2 = vertices[0];
							}
							else {
								v0  = vertices[i - 1];
								vi = vertices[i];
								v2 = vertices[i+1];
							}
							float angle = (v0.sub(vi)).getAngleBetweenVectors(v2.sub(vi));
							if(angle > 180) {
								reflex.add(i);
							}
							else if(angle < 180) {
								convex.add(i);
							}
						}
						
//						Calculating which convex vertices are ears
						for(int i = 0;i < convex.size();i++) {
							Vector v0,vi,v2;
							if(i == 0) {
								v0  = vertices[x[x.length - 1]];
								vi = vertices[0];
								v2 = vertices[1];
							}
							else if(i == x.length - 1) {
								v0  = vertices[i - 1];
								vi = vertices[i];
								v2 = vertices[0];
							}
							else {
								v0  = vertices[i - 1];
								vi = vertices[i];
								v2 = vertices[i+1];
							}
							
							for(int j = 0;j < reflex.size();j++) {
								if(!ModelBuilder.isVertexInsideTriangle(v0, vi, v2, vertices[reflex.get(j)])) {
									ears.add(convex.get(i));
								}
							}
						}
						
						System.out.println(ears.size());
						
					}

				} else {
					facesTriangle.add(x);
				}
			}

		} else {
			facesTriangle = null;
		}

		if (textureFaces != null) {
			for (int[] x : textureFaces) {

				if (x != null && x.length > 3) {

					if (x.length == 4) {
						int[] t1 = new int[3];
						int[] t2 = new int[3];
						t1[0] = x[0];
						t1[1] = x[1];
						t1[2] = x[3];
						t2[0] = x[1];
						t2[1] = x[2];
						t2[2] = x[3];
						textureFacesTriangle.add(t1);
						textureFacesTriangle.add(t2);
					}

				} else {
					textureFacesTriangle.add(x);
				}
			}
		} else {
			textureFacesTriangle = null;
		}

		if (normalFaces != null) {
			for (int[] x : normalFaces) {

				if (x != null && x.length > 3) {

					if (x.length == 4) {
						int[] t1 = new int[3];
						int[] t2 = new int[3];
						t1[0] = x[0];
						t1[1] = x[1];
						t1[2] = x[3];
						t2[0] = x[1];
						t2[1] = x[2];
						t2[2] = x[3];
						normalFacesTriangle.add(t1);
						normalFacesTriangle.add(t2);
					}

				} else {
					normalFacesTriangle.add(x);
				}
			}
		} else {
			normalFacesTriangle = null;
		}

		this.faces = facesTriangle;
		this.textureFaces = textureFacesTriangle;
		this.normalFaces = normalFacesTriangle;

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

	public Vector[] getTextureCoords() {
		return textureCoords;
	}

	public Vector[] getNormals() {
		return normals;
	}

	public List<int[]> getTextureFaces() {
		return textureFaces;
	}

	public List<int[]> getNormalFaces() {
		return normalFaces;
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
