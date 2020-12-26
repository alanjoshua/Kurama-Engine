package Kurama.geometry.MD5;

import Kurama.Math.Vector;
import Kurama.Mesh.Face;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MD5Mesh {
    public String texture;
    public int numVerts;
    public int numTris;
    public int numWeights;

    public List<Weight> weights;
    public List<Vertex> verts;
    public List<Face> triangles;

//    This constructor assumes the reader is pointing to the first line inside mesh {
    public MD5Mesh(BufferedReader reader) throws IOException {
        String line;
        while(!(line = reader.readLine()).equals("}")) {

            var split = line.split("\\s+");

            if (split.length < 2) {
                continue;
            }

            switch (split[1]) {
                case "shader": {
                    texture = split[2];
                    texture = texture.substring(1, texture.length()-1);
                    break;
                }
                case "numverts": {
                    numVerts = Integer.parseInt(split[2]);
                    verts = new ArrayList<>(numVerts);
                    break;
                }
                case "vert": {
                    var vertIndex = Integer.parseInt(split[2]);
                    var texCoord = new Vector(Float.parseFloat(split[4]),Float.parseFloat(split[5]));
                    var startWeight = Integer.parseInt(split[7]);
                    var countWeight = Integer.parseInt(split[8]);
                    verts.add(new Vertex(vertIndex, texCoord, startWeight, countWeight));
                    break;
                }
                case "numtris": {
                    numTris = Integer.parseInt(split[2]);
                    triangles = new ArrayList<>(numTris);
                    break;
                }
                case "tri": {
                    var triIndex = Integer.parseInt(split[2]);
                    var v1 = Integer.parseInt(split[3]);
                    var v2 = Integer.parseInt(split[4]);
                    var v3 = Integer.parseInt(split[5]);
                    triangles.add(new Triangle(triIndex, v1, v2, v3));
                    break;
                }
                case "numweights": {
                    numWeights = Integer.parseInt(split[2]);
                    weights = new ArrayList<>(numWeights);
                    break;
                }
                case "weight": {
                    var weightIndex = Integer.parseInt(split[2]);
                    var joint = Integer.parseInt(split[3]);
                    var bias = Float.parseFloat(split[4]);
                    var pos = new Vector(Float.parseFloat(split[6]), Float.parseFloat(split[7]),
                            Float.parseFloat(split[8]));
                    weights.add(new Weight(weightIndex, joint, bias, pos));
                    break;
                }
                default: {
                    continue;
                }
            }

        }
    }
}
