package Kurama.IOps;

import Kurama.ComponentSystem.components.model.PointCloud;
import Kurama.Math.Vector;
import Kurama.Mesh.Mesh;
import Kurama.Mesh.Meshlet;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static Kurama.Mesh.MeshletGen.getMeshletsInBFOrder;
import static Kurama.utils.Logger.log;

public class PointCloudWriter {

    public static PointCloud loadPointCloud(String fileName, Map<Mesh.VERTATTRIB, List<Vector>> globalVertAttribs,
                                            HashMap<Meshlet, Integer> meshletToIndexMapping) {
        return null;
    }

    public static void writePointCloudToFile(String fileName, PointCloud pointCloud,
                                             Map<Mesh.VERTATTRIB, List<Vector>> globalVertAttribs,
                                             HashMap<Meshlet, Integer> meshletToIndexMapping) throws IOException {

        var keys = globalVertAttribs.keySet();
        int startIndex = pointCloud.root.vertexBegin; // Since the vertices are stored in order in the global list, we can just write all

        log("point cloud keys: "+pointCloud.meshes.get(0).vertAttributes);

        int numVerts = pointCloud.vertexCount;

        int meshletIndOffset = meshletToIndexMapping.values().stream().sorted().collect(Collectors.toList()).get(0);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {

            // Write vertices to file
            for(var key: keys) {
                writer.write(key.label +"\n");
                for(int i = startIndex; i < startIndex + numVerts; i++) {
                    writer.write(globalVertAttribs.get(key).get(i) + "\n");
                }
            }
            writer.write("MESHLETS\n");
            // Write meshlet structures in order
            var meshlets = getMeshletsInBFOrder(pointCloud.root);
            for(var meshlet: meshlets) {
                writer.write((meshletToIndexMapping.get(meshlet) - meshletIndOffset) +"\n");
                writer.write((meshlet.vertexBegin - startIndex) + "\n");
                writer.write(meshlet.vertexCount + "\n");
                writer.write(meshlet.vertexCount + "\n");
                writer.write(meshlet.pos.toString() + "\n");
                writer.write(meshlet.boundRadius + "\n");
                writer.write(meshlet.treeDepth + "\n");
                writer.write(meshletToIndexMapping.get(meshlet.parent)+"\n");

                if(meshlet.children == null || meshlet.children.size() == 0) {
                    writer.write("noChild\n");
                }
                else {
                    for(var child: meshlet.children) {
                        writer.write(meshletToIndexMapping.get(child) + " ");
                    }
                    writer.newLine();
                }
                writer.newLine();
            }
            writer.flush();
        }

    }

}
