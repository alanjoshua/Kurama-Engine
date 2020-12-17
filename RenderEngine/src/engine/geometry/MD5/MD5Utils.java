package engine.geometry.MD5;

import engine.Effects.Material;
import engine.Math.Matrix;
import engine.Math.Quaternion;
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

    public static int MAXWEIGHTSPERVERTEX = 4;

    public static List<AnimationFrame> generateAnimationFrames(MD5AnimModel anim, MD5Model bindModel) {
        List<AnimationFrame> results = new ArrayList<>(anim.numFrames);
        List<Matrix> invmatrices = getInvJointMatrices(bindModel);

        for(var frame: anim.frames) {

            var newFrame = new AnimationFrame(anim.numJoints);
//            var newJoints = new ArrayList<Joint>(anim.numJoints);
            results.add(newFrame);

            for (int i = 0; i < anim.numJoints; i++) {

                var joint = anim.joints.get(i);
                Vector animatedPos = joint.base_pos;
                Vector animatedOrient_temp = joint.base_orient.getPureVec();
                int startIndex = joint.startIndex;

                if ((joint.flags & 1) > 0) {
                    animatedPos.setDataElement(0, frame.components.get(startIndex++));
                }
                if ((joint.flags & 2) > 0) {
                    animatedPos.setDataElement(1, frame.components.get(startIndex++));
                }
                if ((joint.flags & 4) > 0) {
                    animatedPos.setDataElement(2, frame.components.get(startIndex++));
                }
                if ((joint.flags & 8) > 0) {
                    animatedOrient_temp.setDataElement(0, frame.components.get(startIndex++));
                }
                if ((joint.flags & 16) > 0) {
                    animatedOrient_temp.setDataElement(1, frame.components.get(startIndex++));
                }
                if ((joint.flags & 32) > 0) {
                    animatedOrient_temp.setDataElement(2, frame.components.get(startIndex++));
                }

                var animated_orient = Quaternion.calculateWFromXYZ(animatedOrient_temp);
                var newJoint = new Joint(joint.name, joint.parent, animatedPos, animated_orient);
                newFrame.joints.add(newJoint);

//                Not a parent joint
                if(newJoint.parent > -1) {
                    var parentJoint = newFrame.joints.get(newJoint.parent);
                    var rotatedPoint = parentJoint.orient.rotatePoint(animatedPos);
                    newJoint.pos = rotatedPoint.add(parentJoint.pos);
                    newJoint.orient = parentJoint.orient.multiply(animated_orient);
//                    newJoint.orient = parentJoint.orient.getRotationMatrix().matMul(animated_orient.getRotationMatrix())
                    newJoint.orient.normalise();
                }
                newFrame.setMatrix(i, newJoint.pos, newJoint.orient, invmatrices.get(i));
            }

        }

        return results;
    }

    public static List<Matrix> getInvJointMatrices(MD5Model model) {
        List<Matrix> results = new ArrayList<>(model.numJoints);
        for(var joint: model.joints) {
//            Matrix m_ = joint.orient.getInverse().getRotationMatrix();
//            Vector pos_ = (m_.matMul(joint.pos).toVector()).scalarMul(-1);
//            Matrix res = m_.addColumn(pos_);
//            res = res.addRow(new Vector(new float[]{0,0,0,1}));
//            results.add(res);
            var mat = joint.orient.getRotationMatrix().addColumn(joint.pos).addRow(new Vector(0, 0, 0, 1));
            results.add(mat.getInverse());
        }
        return results;
    }

    public static List<Mesh> generateMeshes(MD5Model model, Vector defColor) {
        List<Mesh> results = new ArrayList<>();

        for (MD5Mesh mesh: model.meshes) {
            List<Vector> vertPositions = new ArrayList<>(mesh.numVerts);
            List<Vector> textCoords = new ArrayList<>(mesh.numVerts);
            List<Vector> normals = new ArrayList<>(mesh.numVerts);
            List<Integer> indexList = new ArrayList<>(mesh.numVerts);
            List<Vector> weightBiasesPerVert = new ArrayList<>(mesh.numVerts);
            List<Vector> jointIndicesPerVert = new ArrayList<>(mesh.numVerts);

            for(int i = 0; i < mesh.numVerts; i++) {

                Vector finalVertexPos = new Vector(3, 0);
                Vector weightBias = new Vector(MAXWEIGHTSPERVERTEX, -1);
                Vector jointIndices = new Vector(MAXWEIGHTSPERVERTEX, -1);
                Vertex vert = mesh.verts.get(i);
                normals.add(new Vector(3, 0));

                for(int j = 0;j < vert.countWeight; j++) {
                    var weight = mesh.weights.get(vert.startWeight+j);
                    var joint = model.joints.get(weight.joint);

//                    var transformedPoint = joint.orient.getRotationMatrix().matMul(weight.pos).getColumn(0).add(joint.pos);
                    var transformedPoint = joint.orient.rotatePoint(weight.pos).add(joint.pos).scalarMul(weight.bias);
                    finalVertexPos = finalVertexPos.add(transformedPoint);

                    if(j < MAXWEIGHTSPERVERTEX) {
                        weightBias.setDataElement(j, weight.bias);
                        jointIndices.setDataElement(j, weight.joint);
                    }

                }

                vertPositions.add(finalVertexPos);
                textCoords.add(vert.texCoords);
                weightBiasesPerVert.add(weightBias);
                jointIndicesPerVert.add(jointIndices);
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
            m.setAttribute(weightBiasesPerVert, Mesh.WEIGHTBIASESPERVERT);
            m.setAttribute(jointIndicesPerVert, Mesh.JOINTINDICESPERVERT);
            m.drawMode = GL_TRIANGLES;
            m.meshIdentifier = Utils.getUniqueID();
            m.shouldCull = false;
            results.add(m);
        }

        return results;
    }

}
