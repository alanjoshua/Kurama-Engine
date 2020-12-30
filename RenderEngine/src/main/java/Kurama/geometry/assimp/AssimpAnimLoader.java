package Kurama.geometry.assimp;

import Kurama.Math.Matrix;
import Kurama.Math.Quaternion;
import Kurama.Math.Vector;
import Kurama.Mesh.Material;
import Kurama.Mesh.Mesh;
import Kurama.game.Game;
import Kurama.geometry.MD5.*;
import Kurama.geometry.MeshBuilderHints;
import Kurama.model.AnimatedModel;
import Kurama.utils.Utils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.lwjgl.assimp.Assimp.*;

class Transformation {

    public Quaternion orientation;
    public Vector pos;

    public Transformation(Quaternion orientation, Vector pos) {
        this.orientation = orientation;
        this.pos = pos;
    }

    public Transformation(Matrix transformation) {
        this.pos = transformation.getColumn(3).removeDimensionFromVec(3);
        var rotMatrix = transformation.getSubMatrix(0,0,2,2);
        this.orientation = new Quaternion(rotMatrix);
        this.orientation.normalise();
    }

    public Matrix getTransformationMatrix() {
        Matrix rotationMatrix = this.orientation.getRotationMatrix();

        Matrix transformationMatrix = rotationMatrix.addColumn(this.pos);
        transformationMatrix = transformationMatrix.addRow(new Vector(new float[]{0, 0, 0, 1}));

        return transformationMatrix;
    }

}

public class AssimpAnimLoader {

    public static AnimatedModel load(Game game, String resourcePath, String texturesDir) throws Exception {
        return load(game, resourcePath, texturesDir, aiProcess_JoinIdenticalVertices | aiProcess_Triangulate |
                aiProcess_FixInfacingNormals | aiProcess_LimitBoneWeights);
    }

    public static AnimatedModel load(Game game, String resourcePath, String texturesDir, int flags) throws Exception {
        String fileType = resourcePath.split("\\.")[1];
        AIScene aiScene = aiImportFile(resourcePath, flags);
        if(aiScene == null) {
            throw new Exception("Error loading model");
        }

        int numMaterials = aiScene.mNumMaterials();
        PointerBuffer aiMaterials = aiScene.mMaterials();
        List<Material> materials = new ArrayList<>();

        for(int  i=0; i < numMaterials; i++) {
            AIMaterial aiMat = AIMaterial.create(aiMaterials.get(i));
            var mat = AssimpStaticLoader.processMaterial(aiMat, texturesDir, fileType);
            materials.add(mat);
        }

        List<Bone> boneList = new ArrayList<>();
        int numMeshes = aiScene.mNumMeshes();
        PointerBuffer aiMeshes = aiScene.mMeshes();
        List<Mesh> meshes = new ArrayList<>();
        for(int i = 0;i < numMeshes; i++) {
            var aiMesh = AIMesh.create(aiMeshes.get(i));
            var mesh = processMesh(aiMesh, materials, resourcePath, boneList);
            mesh.meshIdentifier = Utils.getUniqueID();
            meshes.add(mesh);
        }

        List<Matrix> invmatrices = getInvJointMatrices(boneList);

        Node rootNode = buildNodesTree(aiScene.mRootNode(), null);
        var globalInverseTransformation = getTransformation(aiScene.mRootNode().mTransformation()).getTransformationMatrix().getInverse();
        Map<String, Animation> animations = processAnimations(aiScene, boneList, rootNode, globalInverseTransformation, invmatrices);

        String key1 = (String) animations.keySet().toArray()[0];

        AnimatedModel model = new AnimatedModel(game, meshes, animations, animations.get(key1), Utils.getUniqueID());
        return model;
    }

    public static List<Matrix> getInvJointMatrices(List<Bone> boneList) {
        List<Matrix> results = new ArrayList<>(boneList.size());
        for(var joint: boneList) {
            var mat = joint.orient.getRotationMatrix().addColumn(joint.pos).addRow(new Vector(0, 0, 0, 1));
            results.add(mat.getInverse());
        }
        return results;
    }

    public static Map<String, Animation> processAnimations(AIScene aiScene, List<Bone> boneList, Node root,
                                                           Matrix globalInverseTransformation, List<Matrix> invMatrices) {
        Map<String, Animation> animations = new HashMap<>();

        int numAnimations = aiScene.mNumAnimations();
        PointerBuffer aiAnimations = aiScene.mAnimations();
        for(int i = 0;i < numAnimations; i++) {
            AIAnimation aiAnimation = AIAnimation.create(aiAnimations.get(i));
            int maxFrames = calcAnimationMaxFrames(aiAnimation);

            List<AnimationFrame> frames = new ArrayList<>();
            var animation = new Animation(aiAnimation.mName().dataString(), frames, boneList.size(),
                    invMatrices, (float) (aiAnimation.mDuration()/maxFrames));
            animations.put(animation.name, animation);

            for(int j = 0;j < maxFrames; j++) {
                AnimationFrame animatedFrame = new AnimationFrame(boneList.size());
                buildFrameMatrices(aiAnimation, boneList, animatedFrame, j, root,
                        new Transformation(root.orientation, root.pos).getTransformationMatrix(), globalInverseTransformation);
                frames.add(animatedFrame);
            }
        }

        return animations;
    }

    public static void buildFrameMatrices(AIAnimation aiAnimation, List<Bone> boneList, AnimationFrame animatedFrame,
                                          int frame, Node node, Matrix parentTransform, Matrix globalInverseTransform) {

        String nodeName = node.name;
        AINodeAnim aiNodeAnim = findAIAnimNode(aiAnimation, nodeName);
        var nodeTransform = new Transformation(node.orientation, node.pos).getTransformationMatrix();
        if(aiNodeAnim != null) {
            nodeTransform = buildNodeTransformationMatrix(aiNodeAnim, frame).getTransformationMatrix();
        }
        var nodeGlobalTransform = parentTransform.matMul(nodeTransform);

        List<Bone> affectedBones = boneList.stream().filter( b-> b.boneName.equals(nodeName)).collect(Collectors.toList());
        for(Bone bone: affectedBones) {
            var boneTransform = globalInverseTransform.matMul(nodeGlobalTransform).
                    matMul(new Transformation(bone.orient, bone.pos).getTransformationMatrix());
            var finalTrans = new Transformation(boneTransform);
            animatedFrame.joints.add(new Joint(bone.boneName, bone.boneId, finalTrans.pos, finalTrans.orientation));
        }

        for(Node childNode: node.children) {
            buildFrameMatrices(aiAnimation, boneList, animatedFrame, frame, childNode, nodeGlobalTransform, globalInverseTransform);
        }

    }

    private static AINodeAnim findAIAnimNode(AIAnimation aiAnimation, String nodeName) {
        AINodeAnim result = null;
        int numAnimNodes = aiAnimation.mNumChannels();
        PointerBuffer aiChannels = aiAnimation.mChannels();
        for (int i=0; i<numAnimNodes; i++) {
            AINodeAnim aiNodeAnim = AINodeAnim.create(aiChannels.get(i));
            if ( nodeName.equals(aiNodeAnim.mNodeName().dataString())) {
                result = aiNodeAnim;
                break;
            }
        }
        return result;
    }

    private static Transformation buildNodeTransformationMatrix(AINodeAnim aiNodeAnim, int frame) {
        AIVectorKey.Buffer positionKeys = aiNodeAnim.mPositionKeys();
        AIVectorKey.Buffer scalingKeys = aiNodeAnim.mScalingKeys();
        AIQuatKey.Buffer rotationKeys = aiNodeAnim.mRotationKeys();

        AIVectorKey aiVecKey;
        AIVector3D vec;

        Vector pos = null;
        Quaternion orient = null;
        Matrix rotScale = null;

        int numPositions = aiNodeAnim.mNumPositionKeys();
        if (numPositions > 0) {
            aiVecKey = positionKeys.get(Math.min(numPositions - 1, frame));
            vec = aiVecKey.mValue();
            pos = new Vector(vec.x(), vec.y(), vec.z());
//            nodeTransform.translate(vec.x(), vec.y(), vec.z());
        }
        int numRotations = aiNodeAnim.mNumRotationKeys();
        if (numRotations > 0) {
            AIQuatKey quatKey = rotationKeys.get(Math.min(numRotations - 1, frame));
            AIQuaternion aiQuat = quatKey.mValue();
            Quaternion quat = new Quaternion(aiQuat.w(), aiQuat.x(), aiQuat.y(), aiQuat.z());
            rotScale = quat.getRotationMatrix();
        }
        int numScalingKeys = aiNodeAnim.mNumScalingKeys();
        if (numScalingKeys > 0) {
            aiVecKey = scalingKeys.get(Math.min(numScalingKeys - 1, frame));
            vec = aiVecKey.mValue();

            var scale = Matrix.getDiagonalMatrix(new Vector(vec.x(), vec.y(), vec.z()));
            rotScale = scale.matMul(rotScale);
        }

        orient = new Quaternion(rotScale);
        return new Transformation(orient, pos);
    }


    public static int calcAnimationMaxFrames(AIAnimation aiAnimation) {
        int maxFrames = 0;

        int numNodeAnims = aiAnimation.mNumChannels();
        PointerBuffer aiChannels = aiAnimation.mChannels();
        for(int i = 0;i < numNodeAnims; i++) {
            AINodeAnim aiNodeAnim = AINodeAnim.create(aiChannels.get(i));
            int numFrames = Math.max(Math.max(aiNodeAnim.mNumPositionKeys(), aiNodeAnim.mNumScalingKeys()), aiNodeAnim.mNumRotationKeys());
            maxFrames = Math.max(numFrames, maxFrames);
        }

        return maxFrames;
    }

    public static Transformation getTransformation(AIMatrix4x4 input) {
        float[][] mat = new float[4][4];

        mat[0][0] = input.a1();
        mat[0][1] = input.a2();
        mat[0][2] = input.a3();
        mat[0][3] = input.a4();

        mat[1][0] = input.b1();
        mat[1][1] = input.b2();
        mat[1][2] = input.b3();
        mat[1][3] = input.b4();

        mat[2][0] = input.b1();
        mat[2][1] = input.b2();
        mat[2][2] = input.b3();
        mat[2][3] = input.b4();

        mat[3][0] = input.b1();
        mat[3][1] = input.b2();
        mat[3][2] = input.b3();
        mat[3][3] = input.b4();

        return new Transformation(new Matrix(mat));
    }

    public static Node buildNodesTree(AINode aiNode, Node parent) {
        String nodeName = aiNode.mName().dataString();
        var trans = getTransformation(aiNode.mTransformation());
        Node node = new Node(nodeName, parent, trans.pos, trans.orientation);

        int numChildren = aiNode.mNumChildren();
        PointerBuffer aiChildren = aiNode.mChildren();
        for(int i = 0;i < numChildren; i++) {
            AINode aiChildNode = AINode.create(aiChildren.get(i));
            Node childNode = buildNodesTree(aiChildNode, node);
            node.children.add(childNode);
        }
        return node;
    }

    public static Mesh processMesh(AIMesh aiMesh, List<Material> materials, String resourcePath, List<Bone> bonesList) {
        List<List<Vector>> vertAttribs = new ArrayList<>();

        List<Vector> verts = AssimpStaticLoader.processAttribute(aiMesh.mVertices());
        List<Vector> textures = AssimpStaticLoader.processTextureCoords(aiMesh.mTextureCoords(0));
        List<Vector> normals = AssimpStaticLoader.processAttribute(aiMesh.mNormals());
        List<Vector> tangents = AssimpStaticLoader.processAttribute(aiMesh.mTangents());
        List<Vector> biTangents = AssimpStaticLoader.processAttribute(aiMesh.mBitangents());
        List<Integer> indices = AssimpStaticLoader.processIndices(aiMesh);

        List results = processJoints(aiMesh, bonesList);
        List<Vector> jointIndices = (List<Vector>) results.get(0);
        List<Vector> weight = (List<Vector>) results.get(1);

        vertAttribs.add(verts);

        List<Material> meshMaterials = new ArrayList<>();
        var newMat = new Material();
        int matInd = aiMesh.mMaterialIndex();
        if(matInd >= 0 && matInd < materials.size()) {
            newMat = materials.get(matInd);
        }
        meshMaterials.add(newMat);

        var newMesh = new Mesh(indices, null, vertAttribs, meshMaterials, resourcePath, null);
        newMesh.setAttribute(textures, Mesh.TEXTURE);
        newMesh.setAttribute(normals, Mesh.NORMAL);
        newMesh.setAttribute(tangents, Mesh.TANGENT);
        newMesh.setAttribute(biTangents, Mesh.BITANGENT);
        newMesh.setAttribute(jointIndices, Mesh.JOINTINDICESPERVERT);
        newMesh.setAttribute(weight, Mesh.WEIGHTBIASESPERVERT);

        return newMesh;
    }

    public static List processJoints(AIMesh aiMesh, List<Bone> bonesList) {

        List<Vector> jointIndices = new ArrayList<>();
        List<Vector> weights = new ArrayList<>();

        Map<Integer, List<VertexWeight>> weightSet = new HashMap<>();
        int numBones = aiMesh.mNumBones();
        PointerBuffer aiBones = aiMesh.mBones();
        for(int i = 0;i < numBones; i++) {
            AIBone aiBone = AIBone.create(aiBones.get(i));
            int id = bonesList.size();

            Transformation trans = getTransformation(aiBone.mOffsetMatrix());
            Bone bone = new Bone(id, aiBone.mName().dataString(), trans.pos, trans.orientation);
            bonesList.add(bone);
            int numWeights = aiBone.mNumWeights();
            AIVertexWeight.Buffer aiWeights = aiBone.mWeights();
            for(int j = 0;j < numWeights; j++) {
                AIVertexWeight aiWeight = aiWeights.get(j);
                VertexWeight vw = new VertexWeight(bone.boneId, aiWeight.mVertexId(), aiWeight.mWeight());
                weightSet.putIfAbsent(vw.vertexId, new ArrayList<>());
                var weightsList = weightSet.get(vw.vertexId);
                weightsList.add(vw);
            }
        }

        int numVertices = aiMesh.mNumVertices();
        for(int i = 0; i < numVertices; i++) {
            var weightsList = weightSet.get(i);
            int size = weightsList != null ? weightsList.size() : 0;

            Vector tempWeight = new Vector(MD5Utils.MAXWEIGHTSPERVERTEX,0);
            Vector tempJointIndices = new Vector(MD5Utils.MAXWEIGHTSPERVERTEX,0);
            for(int j = 0; j < MD5Utils.MAXWEIGHTSPERVERTEX; j++) {
                if(j < size) {
                    VertexWeight vw = weightsList.get(j);
                    tempWeight.setDataElement(j, vw.weight);
                    tempJointIndices.setDataElement(j, vw.boneId);
                }
            }
            weights.add(tempWeight);
            jointIndices.add(tempJointIndices);
        }

        List res = new ArrayList();
        res.add(jointIndices);
        res.add(weights);
        return res;
    }

    public static int getFlags(MeshBuilderHints hints) {
        int finalFlag = aiProcess_FixInfacingNormals | aiProcess_LimitBoneWeights;
        if(hints.shouldTriangulate) {
            finalFlag |= aiProcess_Triangulate;
        }
        if(hints.shouldReverseWindingOrder) {
            finalFlag |= aiProcess_FlipWindingOrder;
        }
        if(hints.shouldSmartBakeVertexAttributes || hints.shouldDumbBakeVertexAttributes) {
            finalFlag |= aiProcess_JoinIdenticalVertices;
        }
        if(hints.shouldGenerateTangentBiTangent) {
            finalFlag |= aiProcess_CalcTangentSpace;
        }
        return finalFlag;
    }

}
