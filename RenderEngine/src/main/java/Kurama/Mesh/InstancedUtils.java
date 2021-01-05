package Kurama.Mesh;

import Kurama.model.Model;

import java.util.ArrayList;
import java.util.List;


public class InstancedUtils {

//    public int instanceChunkSize;
//    public int instanceDataVBO;
//    public FloatBuffer instanceDataBuffer;

//    public InstancedMesh(List<Integer> indices, List<Face> faces, List<List<Vector>> vertAttributes,
//                         List<Material> materials, String meshLocation, MeshBuilderHints hints, int instanceChunkSize) {
//        super(indices, faces, vertAttributes, materials, meshLocation, hints);
//        this.instanceChunkSize = instanceChunkSize;
//    }
//
//    public InstancedMesh(Mesh mesh, int instanceChunkSize) {
//        super(mesh.indices, mesh.faces, mesh.vertAttributes, mesh.materials, mesh.meshLocation, mesh.hints);
//        this.instanceChunkSize = instanceChunkSize;
//        this.isAnimatedSkeleton = mesh.isAnimatedSkeleton;
//    }
//
//    // Assumes all incoming models have shouldRender property be set to True
//    public List<List<Model>> getRenderChunks(List<Model> models, Predicate<Model> filter) {
//        List<List<Model>> chunks = new ArrayList<>();
//
//        List<Model> currChunk = new ArrayList<>(instanceChunkSize);
//        for(Model m: models) {
//            if(filter.test(m)) {
//                currChunk.add(m);
//            }
//            if(currChunk.size() >= instanceChunkSize) {
//                chunks.add(currChunk);
//                currChunk = new ArrayList<>();
//            }
//        }
//
//        if(currChunk.size() != 0) {
//            chunks.add(currChunk);
//        }
//
//        return chunks;
//    }

    public static List<List<Model>> getRenderChunks(List<Model> models, int chunkSize) {
        List<List<Model>> chunks = new ArrayList<>();

        List<Model> currChunk = new ArrayList<>(chunkSize);
        for(Model m: models) {
            currChunk.add(m);
            if(currChunk.size() >= chunkSize) {
                chunks.add(currChunk);
                currChunk = new ArrayList<>();
            }
        }

        if(currChunk.size() != 0) {
            chunks.add(currChunk);
        }

        return chunks;
    }

}
