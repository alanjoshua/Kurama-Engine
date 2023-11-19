package Kurama.ComponentSystem.components.model;

import Kurama.Mesh.Mesh;
import Kurama.Mesh.Meshlet;
import Kurama.game.Game;

import java.util.List;

public class PointCloud extends Model {

    public Meshlet root;
    public int vertexCount;
    public int maxVertsPerMeshlet;

    public PointCloud(Game game, List<Mesh> meshes, String identifier, int maxVertsPerMeshlet) {
        super(game, meshes, identifier);
        this.maxVertsPerMeshlet = maxVertsPerMeshlet;
        this.shouldSortVertices = true;
    }

    public PointCloud(Game game, Mesh mesh, String identifier, int maxVertsPerMeshlet) {
        super(game, mesh, identifier);
        this.maxVertsPerMeshlet = maxVertsPerMeshlet;
        this.shouldSortVertices = true;
    }
}
