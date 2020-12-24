package engine.model;

import engine.Effects.Material;
import engine.Math.Matrix;
import engine.Math.Quaternion;
import engine.Math.Vector;
import engine.Mesh.Mesh;
import engine.game.Game;
import engine.geometry.MeshBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.lwjgl.opengl.GL11C.GL_LINES;

public class Model {

	public Vector scale;
	public  Vector pos;
	public ModelBehaviour behaviour;
	public Quaternion orientation;
	protected Matrix transformedVertices;
	protected Matrix cacheViewMatrix;
	public boolean isChanged = true;

	public boolean shouldCastShadow = true;
	public String identifier;
	public boolean shouldRender = true;

	public List<Mesh> meshes;
	public Game game;

	// These should be removed. Are here to preserve compatibility with ENED simulation program
	public boolean isCollidable = true;
	public boolean shouldShowCollisionBox = false;
	public boolean shouldShowPath = false;
	public boolean shouldShowAxes = false;
	public Mesh boundingbox;
	public Vector boundingBoxColor;
	public Model pathModel;
	public HashMap<String, List<Material>> materials = new HashMap<>();  	    //List per mesh could only have a maximum of 8 mats.
	public HashMap<String, List<Integer>> matAtlasOffset = new HashMap<>();  //List per mesh could only have a maximum of 8 mats.
	//--------------------------------------------------------------------------------------------

	public Model(Game game, List<Mesh> meshes, String identifier) {
		this.meshes = meshes;
		if(this.meshes == null) {
			this.meshes = new ArrayList<>();
		}

		for(Mesh mesh: meshes) {
			materials.putIfAbsent(mesh.meshIdentifier, new ArrayList<>());
			matAtlasOffset.putIfAbsent(mesh.meshIdentifier, new ArrayList<>());
			if(mesh.materials.size() > 4) {
				throw new IllegalArgumentException("A mesh could only have a maximum of 8 materials");
			}
			for(Material mat: mesh.materials) {
				materials.get(mesh.meshIdentifier).add(mat);
				matAtlasOffset.get(mesh.meshIdentifier).add(0);
			}
		}

		this.game = game;
		scale = new Vector(new float[] { 1, 1, 1 });
		pos = new Vector(3, 0);
		behaviour = null;
		orientation = new Quaternion(new Vector(new float[] { 1, 0, 0, 0 }));
		this.identifier = identifier;
		boundingBoxColor = new Vector(new float[]{1f,1f,1f,1f});
	}

	public void replaceMaterial(String meshID, Material currentMat, Material newMat) {
		for(int i = 0; i < materials.get(meshID).size(); i++) {
			var mat = materials.get(meshID).get(i);
			if (mat.matName.equals(currentMat.matName)) {
				materials.get(meshID).set(i, newMat);
			}
		}

	}

	public void addMesh(Mesh mesh) {
		if(mesh != null) {
			materials.putIfAbsent(mesh.meshIdentifier, new ArrayList<>());
			matAtlasOffset.putIfAbsent(mesh.meshIdentifier, new ArrayList<>());
			if (mesh.materials.size() > 4) {
				throw new IllegalArgumentException("A mesh could only have a maximum of 4 materials");
			}
			for (Material mat : mesh.materials) {
				materials.get(mesh.meshIdentifier).add(mat);
				matAtlasOffset.get(mesh.meshIdentifier).add(0);
			}
		}
	}

	public Model(Game game, Mesh mesh, String identifier) {
		if(mesh != null) {
			this.meshes = Arrays.asList(new Mesh[]{mesh});
		}
		else {
			this.meshes = new ArrayList<>();
		}

		if(mesh != null) {
			materials.putIfAbsent(mesh.meshIdentifier, new ArrayList<>());
			matAtlasOffset.putIfAbsent(mesh.meshIdentifier, new ArrayList<>());
			if (mesh.materials.size() > 8) {
				throw new IllegalArgumentException("A mesh could only have a maximum of 4 materials");
			}
			for (Material mat : mesh.materials) {
				materials.get(mesh.meshIdentifier).add(mat);
				matAtlasOffset.get(mesh.meshIdentifier).add(0);
			}
		}

		this.game = game;
		scale = new Vector(new float[] { 1, 1, 1 });
		pos = new Vector(3, 0);
		behaviour = null;
		orientation = new Quaternion(new Vector(new float[] { 1, 0, 0, 0 }));
		this.identifier = identifier;
		boundingBoxColor = new Vector(new float[]{1f,1f,1f,1f});
	}

	public void tick(ModelBehaviourTickInput params) {
		if (behaviour != null) {
			behaviour.tick(this,params);
		}
	}

	public Vector getDirectionToFrontFromCentre(Model search) {
		Vector[] bounds = Model.getBounds(search.boundingbox);
		float deltaZ = (bounds[1].get(2) - bounds[0].get(2)) / 2f;

		Vector z = search.getOrientation().getRotationMatrix().getColumn(2);
		return z.scalarMul(deltaZ);
	}

	public static Vector[] getBounds(Mesh mesh) {
		return Model.getBounds(mesh.getVertices());
	}

	public static Vector[] getBounds(List<Vector> verts) {
		int dimensions = verts.get(0).getNumberOfDimensions();
		Vector boundMin = new Vector(dimensions,Float.POSITIVE_INFINITY);
		Vector boundMax = new Vector(dimensions,Float.NEGATIVE_INFINITY);

		for(Vector v: verts) {
			for(int i = 0;i < dimensions;i++) {
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

	public Mesh getBoundingBox() {
		return boundingbox;
	}

	public void setBoundingBoxColor(Vector color) {
		this.boundingBoxColor = color;
		MeshBuilder.addColor(boundingbox,boundingBoxColor);
		boundingbox.drawMode = GL_LINES;
	}

	public void setRandomColorToBoundingBox() {
		MeshBuilder.addRandomColor(boundingbox);
		boundingbox.drawMode = GL_LINES;
	}

	public Matrix getObjectToWorldMatrix() {
//		if(isChanged) {
			Matrix rotationMatrix = this.orientation.getRotationMatrix();
			Matrix scalingMatrix = Matrix.getDiagonalMatrix(this.getScale());
			Matrix rotScalMatrix = rotationMatrix.matMul(scalingMatrix);

			Matrix transformationMatrix = rotScalMatrix.addColumn(this.pos);
			transformationMatrix = transformationMatrix.addRow(new Vector(new float[]{0, 0, 0, 1}));
//			isChanged = false;
//			cacheViewMatrix = transformationMatrix;
//		}
		return transformationMatrix;
	}

	public Matrix getWorldToObject() {
		Matrix m_ = orientation.getInverse().getRotationMatrix();
		Vector pos_ = (m_.matMul(pos).toVector()).scalarMul(-1);
		Matrix res = m_.addColumn(pos_);
		res = res.addRow(new Vector(new float[]{0,0,0,1}));
		return res;
	}

	public ModelBehaviour getBehaviour() {
		return behaviour;
	}

	public void setBehaviour(ModelBehaviour behaviour) {
		this.behaviour = behaviour;
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

//	public Vector getBoundMin() {
//		return boundMin;
//	}
//
//	public Vector getBoundMax() {
//		return boundMax;
//	}

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

//	public Vector getCentre() {
//		Vector res = new Vector(new float[]{0,0,0});
//		for(Vector v: mesh.getVertices()) {
//			res = res.add(v.removeDimensionFromVec(3).mul(scale));
//		}
//		res = res.scalarMul(1f/mesh.getVertices().size());
//		res = res.add(pos);
//
//		return res;
//	}

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
		meshes.stream().forEach(m -> m.cleanUp());

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
