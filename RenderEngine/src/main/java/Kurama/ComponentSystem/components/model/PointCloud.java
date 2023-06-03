package Kurama.ComponentSystem.components.model;

import Kurama.Mesh.Mesh;
import Kurama.Mesh.Meshlet;
import Kurama.game.Game;

import java.util.List;

public class PointCloud extends Model {

    public Meshlet root;
    public int vertexCount;
    public PointCloud(Game game, List<Mesh> meshes, String identifier) {
        super(game, meshes, identifier);
    }

    public PointCloud(Game game, Mesh mesh, String identifier) {
        super(game, mesh, identifier);
    }
}
