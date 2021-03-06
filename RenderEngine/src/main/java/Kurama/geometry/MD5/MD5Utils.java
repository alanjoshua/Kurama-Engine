package Kurama.geometry.MD5;

import Kurama.Math.Quaternion;
import Kurama.Math.Transformation;
import Kurama.Math.Vector;
import Kurama.Mesh.*;
import Kurama.geometry.MeshBuilder;
import Kurama.geometry.MeshBuilderHints;
import Kurama.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MD5Utils {

    public static int MAXWEIGHTSPERVERTEX = 4;

    public static List<AnimationFrame> generateAnimationFrames(MD5AnimModel anim, MD5Model bindModel) {
        List<AnimationFrame> animationFrames = new ArrayList<>(anim.numFrames);
        List<Transformation> invTransList = getInvJointMatrices(bindModel);

        for(var frame: anim.frames) {

            var newFrame = new AnimationFrame(anim.numJoints);
            animationFrames.add(newFrame);

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
            }

            for (int i = 0; i < anim.numJoints; i++) {
                var invTrans = invTransList.get(i);
                var currentJoint = newFrame.joints.get(i);
                var res = new Transformation(currentJoint).matMul(invTrans);
                currentJoint.pos = res.pos;
                currentJoint.orient = res.orientation;
                currentJoint.scale = res.scale;
            }

        }

        return animationFrames;
    }

    public static List<Transformation> getInvJointMatrices(MD5Model model) {

        List<Transformation> transRes = new ArrayList<>(model.numJoints);

        for(var joint: model.joints) {
            var inv_orient = joint.orient.getInverse();
            var inv_pos = inv_orient.rotatePoint(joint.pos).scalarMul(-1);
            var trans = new Transformation(inv_orient, inv_pos);
            transRes.add(trans);
        }

        return transRes;
    }

    public static List<Mesh> generateMeshes(MD5Model model, Vector defColor, MeshBuilderHints hints) {
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
            for(var v: vertPositions) {
                matList.add(new Vector(new float[]{0}));
            }

            if(mesh.texture != null) {
                try {
                    mats.get(0).texture = new Texture(mesh.texture);
                    mats.get(0).diffuseMap = mats.get(0).texture;
                    mats.get(0).specularMap = mats.get(0).texture;
                }
                catch (Exception e) {
                    mats.get(0).ambientColor = defColor;
                    mats.get(0).diffuseColor = defColor;
                    mats.get(0).specularColor = defColor;
                }
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
            m.setAttribute(weightBiasesPerVert, Mesh.WEIGHTBIASESPERVERT);
            m.setAttribute(jointIndicesPerVert, Mesh.JOINTINDICESPERVERT);
            m.setAttribute(matList, Mesh.MATERIAL);
            m = MeshBuilder.bakeMesh(m, null);

            if(hints != null && hints.isInstanced) {
//                m = new InstancedMesh(m, hints.numInstances);
                m.isInstanced = true;
                m.instanceChunkSize = hints.numInstances;
            }

            m.meshIdentifier = Utils.getUniqueID();
            m.shouldCull = false;
            results.add(m);
        }

        return results;
    }

}
