package models;

import java.util.ArrayList;
import java.util.List;

import Math.Matrix;
import Math.Quaternion;
import Math.Vector;
import models.DataStructure.Mesh.Mesh;

public class Model {

	public interface Tick {
		void tick(Model m);
	}

	protected List<Vector> rotation;
//	protected Vector[] vertices;
//	protected Vector[] textureCoords;
//	protected Vector[] normals;
//	protected List<int[]> faces;
//	protected List<int[]> textureFaces;
//	protected List<int[]> normalFaces;

	protected Vector scale;
	protected Vector pos;
	protected Tick tickObj;
	protected Quaternion orientation;
	protected Matrix transformedVertices;
	protected boolean isChanged = true;

	public Mesh mesh;

//	public Model(Vector[] vertices, List<int[]> faces, Vector[] textureCoords, List<int[]> textureFaces,
//			Vector[] normals, List<int[]> normalFaces) {
//		this.vertices = vertices;
//		this.faces = faces;
//
//		this.textureFaces = textureFaces;
//		this.textureCoords = textureCoords;
//
//		this.normalFaces = normalFaces;
//		this.normals = normals;
//
//		rotation = new ArrayList<Vector>();
//		scale = new Vector(new float[] { 1, 1, 1 });
//		pos = new Vector(3, 0);
//		tickObj = null;
//		orientation = new Quaternion(new Vector(new float[] { 1, 0, 0, 0 }));
//	}

	public Model(Mesh mesh) {
		this.mesh = mesh;
		rotation = new ArrayList<Vector>();
		scale = new Vector(new float[] { 1, 1, 1 });
		pos = new Vector(3, 0);
		tickObj = null;
		orientation = new Quaternion(new Vector(new float[] { 1, 0, 0, 0 }));
	}

	public void tick() {
		if (tickObj != null) {
			tickObj.tick(this);
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

//		List<int[]> faces = (List<int[]>) triangulatedData.get(0);
//		List<int[]> normalFaces = (List<int[]>) triangulatedData.get(2);
//		List<int[]> textureFaces = (List<int[]>) triangulatedData.get(1);
//
//		List<Face> newFaces = new ArrayList<>(faces.size());
//
//		for(int i = 0;i < faces.size();i++) {
//			Face tempFace = new Face();
//
//			for(int j = 0;j < faces.get(i).length;j++) {
//
//				Vertex tempVert = new Vertex();
//				tempVert.setAttribute(faces.get(i)[j],Vertex.POSITION);
//
//				if(textureFaces != null && i < textureFaces.size()) {
//					if(textureFaces.get(i) != null) {
//						tempVert.setAttribute(textureFaces.get(i)[j],Vertex.TEXTURE);
//					}
//				}
//
//				if(normalFaces != null && i < normalFaces.size()) {
//					if(normalFaces.get(i) != null) {
//						tempVert.setAttribute(normalFaces.get(i)[j],Vertex.NORMAL);
//					}
//				}
//				tempFace.vertices.add(tempVert);
//			}
//			newFaces.add(tempFace);
//		}
//
//		mesh.faces = newFaces;
//		mesh.trimEverything();
	}

	public void triangulate(boolean forceUseEarClipping) {
		mesh = ModelBuilder.triangulate(mesh,forceUseEarClipping);
	}

	public Mesh getMesh() {return mesh;}

	public Tick getTickObj() {
		return tickObj;
	}

	public void setTickObj(Tick tickObj) {
		this.tickObj = tickObj;
	}

	public Vector getPos() {
		return pos;
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

//	public Vector[] getVertices() {
//		return vertices;
//	}
//
//	public List<int[]> getFaces() {
//		return faces;
//	}
//
//	public Vector[] getTextureCoords() {
//		return textureCoords;
//	}
//
//	public Vector[] getNormals() {
//		return normals;
//	}
//
//	public List<int[]> getTextureFaces() {
//		return textureFaces;
//	}
//
//	public List<int[]> getNormalFaces() {
//		return normalFaces;
//	}

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
