package Kurama.ComponentSystem.components.model;

import Kurama.ComponentSystem.components.Component;
import Kurama.Math.Quaternion;
import Kurama.Math.Vector;
import Kurama.Mesh.Material;
import Kurama.Mesh.Mesh;
import Kurama.game.Game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Model extends SceneComponent {

	public List<Mesh> meshes;

	public HashMap<String, List<Material>> materials = new HashMap<>();  	    //List per mesh could only have a maximum of 8 mats.
	public HashMap<String, List<Integer>> matAtlasOffset = new HashMap<>();  //List per mesh could only have a maximum of 8 mats.
	//--------------------------------------------------------------------------------------------

	// Useful when the user already knows that the point cloud is sorted, or is using the point cloud functionality for something else that does not require sorting
	public boolean shouldSortVertices = false;

	public Model(Game game, List<Mesh> meshes, String identifier) {
		super(game, null, identifier);
		this.meshes = meshes;
		if(this.meshes == null) {
			this.meshes = new ArrayList<>();
		}

		for (Mesh mesh : this.meshes) {
			materials.putIfAbsent(mesh.meshIdentifier, new ArrayList<>());
			matAtlasOffset.putIfAbsent(mesh.meshIdentifier, new ArrayList<>());

			if (mesh.materials.size() > 4) {
				throw new IllegalArgumentException("A mesh could only have a maximum of 8 materials");
			}
			for (Material mat : mesh.materials) {
				materials.get(mesh.meshIdentifier).add(mat);
				matAtlasOffset.get(mesh.meshIdentifier).add(0);
			}
		}

	}

	public Model(Game game, Component parent, List<Mesh> meshes, String identifier) {
		super(game, parent, identifier);
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
	}

	public Model(Game game, Mesh mesh, String identifier) {
		super(game, null, identifier);
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
	}

	public Model(Game game, Component parent, Mesh mesh, String identifier) {
		super(game, parent, identifier);
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

	public Vector getPos() {
		return pos;
	}

	public void setPos(Vector pos) {
		this.pos = pos;
	}

	public void setPos(float x, float y, float z) {
		Vector pos = new Vector(new float[]{x,y,z});
		this.pos = pos;
	}

	public Vector getScale() {
		return super.getScale();
	}


	public void setScale(float v) {
		Vector scale = new Vector(new float[]{v,v,v});
		super.setScale(scale);
	}

	public void setScale(float x, float y, float z) {
		Vector scale = new Vector(new float[]{x,y,z});
		super.setScale(scale);
	}

	public Quaternion getOrientation() {
		return orientation;
	}

	public void setOrientation(Quaternion quaternion) {
		this.orientation = quaternion;
	}

	public void cleanUp() {
		meshes.stream().forEach(m -> m.cleanUp());
	}

	@Override
	public String toString() {
		return identifier;
	}

}
