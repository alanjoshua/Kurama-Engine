package engine.geometry.MD5;

import engine.Effects.Material;
import engine.Math.Vector;
import engine.Mesh.Mesh;
import engine.misc_structures.Texture;
import engine.utils.Logger;
import engine.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class MD5Utils {

    public static List<Mesh> generateMeshes(MD5Model model, Vector defColor) {
        List<Mesh> results = new ArrayList<>();

        for (MD5Mesh mesh: model.meshes) {
            List<Vector> vertPositions = new ArrayList<>(mesh.numVerts);
            List<Vector> textCoords = new ArrayList<>(mesh.numVerts);
            List<Integer> indexList = new ArrayList<>(mesh.numVerts);

            for(int i = 0; i < mesh.numVerts; i++) {
                Vector finalVertexPos = new Vector(3, 0);

                for(int j = 0;j < mesh.verts.get(i).countWeight; j++) {
                    var weight = mesh.weights.get(mesh.verts.get(i).startWeight + j);
                    var joint = model.joints.get(weight.joint);

                    var trans = joint.orient.rotatePoint(weight.pos);
                    finalVertexPos = finalVertexPos.add((joint.pos.add(trans)).scalarMul(weight.bias));
                }

                vertPositions.add(finalVertexPos);
                textCoords.add(mesh.verts.get(i).texCoords);
                indexList.add(i);
            }

            List<List<Vector>> vertAttribs = new ArrayList<>();
            List<Material> mats = new ArrayList<>();
            mats.add(new Material());
            List<Vector> matList = new ArrayList<>();
            matList.add(new Vector(new float[]{0}));

            if(mesh.texture != null) {
                mats.get(0).texture = new Texture(mesh.texture);
                Logger.log("mat tex name: "+mesh.texture);
            }

            Mesh m = new Mesh(indexList, mesh.triangles, vertAttribs, mats, null, null);
            m.setAttribute(vertPositions, Mesh.POSITION);
            m.setAttribute(textCoords, Mesh.TEXTURE);
            m.setAttribute(matList,Mesh.MATERIAL);
            m.meshIdentifier = Utils.getUniqueID();

            results.add(m);
        }

        return results;
    }

}
