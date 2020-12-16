package engine.geometry.MD5;

import engine.Effects.Material;
import engine.Math.Vector;
import engine.Mesh.Face;
import engine.Mesh.Mesh;
import engine.misc_structures.Texture;
import engine.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;

public class MD5Utils {

    public static List<Mesh> generateMeshes(MD5Model model, Vector defColor) {
        List<Mesh> results = new ArrayList<>();

        for (MD5Mesh mesh: model.meshes) {
            List<Vector> vertPositions = new ArrayList<>(mesh.numVerts);
            List<Vector> textCoords = new ArrayList<>(mesh.numVerts);
            List<Vector> normals = new ArrayList<>(mesh.numVerts);
            List<Integer> indexList = new ArrayList<>(mesh.numVerts);

            for(int i = 0; i < mesh.numVerts; i++) {

                Vector finalVertexPos = new Vector(3, 0);
                Vertex vert = mesh.verts.get(i);
                normals.add(new Vector(3, 0));

                for(int j = vert.startWeight;j < vert.startWeight + vert.countWeight; j++) {
                    var weight = mesh.weights.get(j);
                    var joint = model.joints.get(weight.joint);

//                    var transformedPoint = joint.orient.getRotationMatrix().matMul(weight.pos).getColumn(0).add(joint.pos);
                    var transformedPoint = joint.orient.rotatePoint(weight.pos).add(joint.pos).scalarMul(weight.bias);
                    finalVertexPos = finalVertexPos.add(transformedPoint);
                }

                vertPositions.add(finalVertexPos);
                textCoords.add(vert.texCoords);
            }

            for(Face t: mesh.triangles) {
                indexList.add(t.get(0));
                indexList.add(t.get(1));
                indexList.add(t.get(2));

                var p0 = vertPositions.get(t.get(0));
                var p1 = vertPositions.get(t.get(1));
                var p2 = vertPositions.get(t.get(2));

                var normal = p2.sub(p0).cross(p1.sub(p0));
                normals.set(t.get(0), normals.get(t.get(0)).add(normal));
                normals.set(t.get(1), normals.get(t.get(1)).add(normal));
                normals.set(t.get(2), normals.get(t.get(2)).add(normal));

//                normals.get(t.get(0)).add(normal);
//                normals.get(t.get(1)).add(normal);
//                normals.get(t.get(2)).add(normal);
            }

            for(int i = 0;i < normals.size();i++) {
                normals.set(i, normals.get(i).normalise());
//                Logger.log(normals.get(i));
            }

            List<List<Vector>> vertAttribs = new ArrayList<>();
            List<Material> mats = new ArrayList<>();
            mats.add(new Material());
            List<Vector> matList = new ArrayList<>();
            matList.add(new Vector(new float[]{0}));

            if(mesh.texture != null) {
                mats.get(0).texture = new Texture(mesh.texture);
                mats.get(0).diffuseMap = mats.get(0).texture;
                mats.get(0).specularMap = mats.get(0).texture;
            }
            else {
                mats.get(0).ambientColor = defColor;
                mats.get(0).diffuseColor = defColor;
                mats.get(0).specularColor = defColor;
            }

            var tempSplit = mesh.texture.split("\\.");
            var fileFormat = tempSplit[1];
            var normalFile = tempSplit[0]+"_normal."+fileFormat;

            if(new File(normalFile).exists()) {
                mats.get(0).normalMap = new Texture(normalFile);
            }

            Mesh m = new Mesh(indexList, mesh.triangles, vertAttribs, mats, null, null);
            m.setAttribute(vertPositions, Mesh.POSITION);
            m.setAttribute(textCoords, Mesh.TEXTURE);
            m.setAttribute(normals, Mesh.NORMAL);
            m.setAttribute(matList,Mesh.MATERIAL);
            m.drawMode = GL_TRIANGLES;
            m.meshIdentifier = Utils.getUniqueID();
            m.shouldCull = false;
            results.add(m);
        }

        return results;
    }

}
