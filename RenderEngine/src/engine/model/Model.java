package engine.model;

import engine.DataStructure.Mesh.Face;
import engine.DataStructure.Mesh.Vertex;
import engine.Math.Matrix;
import engine.Math.Quaternion;
import engine.Math.Vector;
import engine.DataStructure.Mesh.Mesh;
import engine.game.Game;
import org.lwjgl.system.CallbackI;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11C.GL_LINES;

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
	protected Vector boundMin;
	protected  Vector boundMax;
	public boolean isCollidable = true;
	public boolean shouldShowCollisionBox = false;
	public boolean shouldShowPath = false;
	public Mesh boundingbox;
	public Vector boundingBoxColor;
	public Model pathModel;

	public String identifier;

	public Mesh mesh;
	protected Game game;

	public Model(Game game, Mesh mesh, String identifier) {
		this.mesh = mesh;
		this.game = game;
		scale = new Vector(new float[] { 1, 1, 1 });
		pos = new Vector(3, 0);
		miniBehaviourObj = null;
		orientation = new Quaternion(new Vector(new float[] { 1, 0, 0, 0 }));
		this.identifier = identifier;

		boundingBoxColor = new Vector(new float[]{1f,1f,1f,1f});
		calculateBoundingBox();
	}

	public Model(Game game, Mesh mesh, String identifier,boolean shouldCreateBoundingBox) {
		this.mesh = mesh;
		this.game = game;
		scale = new Vector(new float[] { 1, 1, 1 });
		pos = new Vector(3, 0);
		miniBehaviourObj = null;
		orientation = new Quaternion(new Vector(new float[] { 1, 0, 0, 0 }));
		this.identifier = identifier;

		boundingBoxColor = new Vector(new float[]{1f,1f,1f,1f});
	}

	public void tick(ModelTickInput params) {
		if (miniBehaviourObj != null) {
			miniBehaviourObj.tick(this,params);
		}
	}

	public static Vector[] getBounds(Mesh mesh) {
		Vector boundMin = new Vector(new float[]{Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY});
		Vector boundMax = new Vector(new float[]{Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY});

		for(Vector v: mesh.getVertices()) {
			for(int i = 0;i < 3;i++) {
				if(v.get(i) < boundMin.get(i)) {
					boundMin.setDataElement(i,v.get(i));
				}
				if(v.get(i) > boundMax.get(i)) {
					boundMax.setDataElement(i,v.get(i));
				}
			}
		}

		Vector[] res = new Vector[2];
		res[0] = boundMin;
		res[1] = boundMax;
		return res;
	}

	public static Vector[] getBounds(List<Vector> verts) {
		Vector boundMin = new Vector(new float[]{Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY});
		Vector boundMax = new Vector(new float[]{Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY});

		for(Vector v: verts) {
			for(int i = 0;i < 3;i++) {
				if(v.get(i) < boundMin.get(i)) {
					boundMin.setDataElement(i,v.get(i));
				}
				if(v.get(i) > boundMax.get(i)) {
					boundMax.setDataElement(i,v.get(i));
				}
			}
		}

		Vector[] res = new Vector[2];
		res[0] = boundMin;
		res[1] = boundMax;
		return res;
	}

	public void calculateBoundingBox() {
//		boundMin = new Vector(new float[]{Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY});
//		boundMax = new Vector(new float[]{Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY});
//
//		for(Vector v: mesh.getVertices()) {
//			for(int i = 0;i < 3;i++) {
//				if(v.get(i) < boundMin.get(i)) {
//					boundMin.setDataElement(i,v.get(i));
//				}
//				if(v.get(i) > boundMax.get(i)) {
//					boundMax.setDataElement(i,v.get(i));
//				}
//			}
//		}

		Vector[] bounds = Model.getBounds(mesh);
		boundMin = bounds[0];
		boundMax = bounds[1];

//		Code to create bounding box mesh

		Vector v1 = new Vector(boundMin.getData()).addDimensionToVec(1);
		Vector v7 = new Vector(boundMax.getData()).addDimensionToVec(1);

		Vector v2 = new Vector(new float[]{v7.get(0),v1.get(1),v1.get(2),1});
		Vector v3 = new Vector(new float[]{v7.get(0),v1.get(1),v7.get(2),1});
		Vector v4 = new Vector(new float[]{v1.get(0),v1.get(1),v7.get(2),1});

		Vector v5 = new Vector(new float[]{v1.get(0),v7.get(1),v1.get(2),1});
		Vector v6 = new Vector(new float[]{v7.get(0),v7.get(1),v1.get(2),1});
		Vector v8 = new Vector(new float[]{v1.get(0),v7.get(1),v7.get(2),1});

		List<Vector> vertices = new ArrayList<>();
		vertices.add(v1);
		vertices.add(v2);
		vertices.add(v2);
		vertices.add(v3);
		vertices.add(v3);
		vertices.add(v4);
		vertices.add(v4);
		vertices.add(v1);

		vertices.add(v5);
		vertices.add(v6);
		vertices.add(v6);
		vertices.add(v7);
		vertices.add(v7);
		vertices.add(v8);
		vertices.add(v8);
		vertices.add(v5);

		vertices.add(v1);
		vertices.add(v5);
		vertices.add(v2);
		vertices.add(v6);
		vertices.add(v3);
		vertices.add(v7);
		vertices.add(v4);
		vertices.add(v8);

		List<Face> faces = new ArrayList<>(8);

		for(int i = 0;i < 4;i++) {
			Face tempFace = new Face();
			Vertex vert1 = new Vertex();
			Vertex vert2 = new Vertex();
			if(i != 3) {
				vert1.setAttribute(i,Vertex.POSITION);
				vert2.setAttribute(i+1,Vertex.POSITION);
			}else {
				vert1.setAttribute(i,Vertex.POSITION);
				vert2.setAttribute(0,Vertex.POSITION);
			}
			tempFace.addVertex(vert1);
			tempFace.addVertex(vert2);
			faces.add(tempFace);
		}

		for(int i = 4;i < 8;i++) {
			Face tempFace = new Face();
			Vertex vert1 = new Vertex();
			Vertex vert2 = new Vertex();
			if(i != 7) {
				vert1.setAttribute(i, Vertex.POSITION);
				vert2.setAttribute(i+1,Vertex.POSITION);
			}else {
				vert1.setAttribute(i,Vertex.POSITION);
				vert2.setAttribute(i+4,Vertex.POSITION);
			}
			tempFace.addVertex(vert1);
			tempFace.addVertex(vert2);
			faces.add(tempFace);
		}

		Face tempFace = new Face();
		Vertex vert1 = new Vertex();
		Vertex vert2 = new Vertex();
		vert1.setAttribute(0,Vertex.POSITION);
		vert2.setAttribute(4,Vertex.POSITION);
		tempFace.addVertex(vert1);
		tempFace.addVertex(vert2);
		faces.add(tempFace);

		tempFace = new Face();
		vert1 = new Vertex();
		vert2 = new Vertex();
		vert1.setAttribute(1,Vertex.POSITION);
		vert2.setAttribute(5,Vertex.POSITION);
		tempFace.addVertex(vert1);
		tempFace.addVertex(vert2);
		faces.add(tempFace);

		tempFace = new Face();
		vert1 = new Vertex();
		vert2 = new Vertex();
		vert1.setAttribute(2,Vertex.POSITION);
		vert2.setAttribute(6,Vertex.POSITION);
		tempFace.addVertex(vert1);
		tempFace.addVertex(vert2);
		faces.add(tempFace);

		tempFace = new Face();
		vert1 = new Vertex();
		vert2 = new Vertex();
		vert1.setAttribute(3,Vertex.POSITION);
		vert2.setAttribute(7,Vertex.POSITION);
		tempFace.addVertex(vert1);
		tempFace.addVertex(vert2);
		faces.add(tempFace);

		List<List<Vector>> vertAttribs = new ArrayList<>(1);
		vertAttribs.add(vertices);

		boundingbox = new Mesh(null,faces,vertAttribs);
		ModelBuilder.addColor(boundingbox,boundingBoxColor);
		boundingbox.drawMode = GL_LINES;
		boundingbox.initOpenGLMeshData();
	}

	public Matrix getObjectToWorldMatrix() {

		Matrix rotationMatrix = this.orientation.getRotationMatrix();
		Matrix scalingMatrix = Matrix.getDiagonalMatrix(this.getScale());
		Matrix rotScalMatrix = rotationMatrix.matMul(scalingMatrix);

		Matrix transformationMatrix = rotScalMatrix.addColumn(this.pos);
		transformationMatrix = transformationMatrix.addRow(new Vector(new float[] { 0, 0, 0, 1 }));
		return transformationMatrix;
	}

	public Matrix getWorldToObject() {
		Matrix m_ = orientation.getInverse().getRotationMatrix();
		Vector pos_ = (m_.matMul(pos).toVector()).scalarMul(-1);
		Matrix res = m_.addColumn(pos_);
		res = res.addRow(new Vector(new float[]{0,0,0,1}));
		return res;
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

	public Vector getBoundMin() {
		return boundMin;
	}

	public Vector getBoundMax() {
		return boundMax;
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

	public void cleanUp() {
		mesh.cleanUp();
		if(boundingbox!=null) {
			boundingbox.cleanUp();
		}
		if(pathModel!=null) {
			pathModel.cleanUp();
		}
	}

	@Override
	public String toString() {
		return identifier;
	}

}
