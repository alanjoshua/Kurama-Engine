package Kurama.geometry.assimp;

public class VertexWeight {

    public int boneId;
    public int vertexId;
    public float weight;

    public VertexWeight(int boneId, int vertexId, float weight) {
        this.boneId = boneId;
        this.vertexId = vertexId;
        this.weight = weight;
    }

}
