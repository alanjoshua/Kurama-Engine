package Kurama.IOps;

import Kurama.ComponentSystem.components.model.PointCloud;
import Kurama.Math.Vector;
import Kurama.Mesh.Mesh;
import Kurama.Mesh.Meshlet;
import main.PointCloudRenderer;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static Kurama.Mesh.Mesh.VERTATTRIB.COLOR;
import static Kurama.Mesh.Mesh.VERTATTRIB.POSITION;
import static Kurama.Mesh.MeshletGen.getMeshletsInBFOrder;
import static Kurama.utils.Logger.log;

public class PointCloudIO {

    public static PointCloud loadPointCloud(String fileName, PointCloudRenderer renderer) {

        String currentlyReadingField = "";

        int meshletsIndOffset = renderer.meshlets.size();
        int vertexIndOffset = renderer.globalVertAttribs.get(POSITION).size();
        var meshlets = new ArrayList<Meshlet>();

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String curLine = null;

            while((curLine = reader.readLine()) != null) {

                if(curLine.equalsIgnoreCase("pos")) {
                    currentlyReadingField = "POS";
                    continue;
                }

                if(curLine.equalsIgnoreCase("color")) {
                    currentlyReadingField = "COLOR";
                    continue;
                }

                if(curLine.equalsIgnoreCase("MESHLETS")) {
                    currentlyReadingField = "MESHLETS";
                    continue;
                }

                if(currentlyReadingField == "POS") {
                    String[] tokens = curLine.split(" ");
                    renderer.globalVertAttribs.get(POSITION).add(new Vector(Float.valueOf(tokens[0]), Float.valueOf(tokens[1]), Float.valueOf(tokens[2])));
                }
                else if(currentlyReadingField == "COLOR") {
                    String[] tokens = curLine.split(" ");
                    renderer.globalVertAttribs.get(COLOR).add(new Vector(Float.valueOf(tokens[0]), Float.valueOf(tokens[1]), Float.valueOf(tokens[2]), Float.valueOf(tokens[3])));
                }

                else if(currentlyReadingField == "MESHLETS") {

                    var newMeshlet = new Meshlet();

                    var meshletId = Integer.valueOf(curLine) + meshletsIndOffset;

                    newMeshlet.vertexBegin = Integer.valueOf(reader.readLine()) + vertexIndOffset;
                    newMeshlet.vertexCount = Integer.valueOf(reader.readLine());

                    var posTokens = reader.readLine().split(" ");
                    newMeshlet.pos = new Vector(new Vector(Float.valueOf(posTokens[0]), Float.valueOf(posTokens[1]), Float.valueOf(posTokens[2])));

                    newMeshlet.boundRadius = Float.valueOf(reader.readLine());
                    newMeshlet.treeDepth = Integer.valueOf(reader.readLine());

                    var parentId = Integer.valueOf(reader.readLine()) + meshletsIndOffset;

                    if(parentId == meshletId) {
                        newMeshlet.parent = newMeshlet;
                    }
                    else {
                        newMeshlet.parent = renderer.meshlets.get(parentId);
                    }

                    var childString = reader.readLine();
                    newMeshlet.childrenIndices = new ArrayList<>();
                    if(!childString.equalsIgnoreCase("noChild")) {
                        var childTokens = childString.split(" ");
                        for(var token: childTokens) {
                            newMeshlet.childrenIndices.add(Integer.valueOf(token) + meshletsIndOffset);
                        }
                    }
                    reader.readLine(); // Skip next line which is supposed to just be empty

                    renderer.addMeshlet(newMeshlet);
                    meshlets.add(newMeshlet);
                }
            }

            // Set the child node links
            for(var meshlet: meshlets) {
                for(var child: meshlet.childrenIndices) {
                    meshlet.children.add(renderer.meshlets.get(child));
                }
                meshlet.childrenIndices = null;
            }

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    public static void writePointCloudToFile(String fileName, PointCloud pointCloud,
                                             Map<Mesh.VERTATTRIB, List<Vector>> globalVertAttribs,
                                             HashMap<Meshlet, Integer> meshletToIndexMapping) {

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
                writer.write(meshlet.pos.toString() + "\n");
                writer.write(meshlet.boundRadius + "\n");
                writer.write(meshlet.treeDepth + "\n");
                writer.write((meshletToIndexMapping.get(meshlet.parent) - meshletIndOffset) +"\n");

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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
