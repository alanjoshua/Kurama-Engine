package Kurama.geometry.assimp;

import Kurama.Math.Matrix;
import Kurama.Math.Quaternion;
import Kurama.Math.Transformation;
import Kurama.Math.Vector;
import Kurama.Mesh.Material;
import Kurama.Mesh.Mesh;
import Kurama.game.Game;
import Kurama.geometry.MD5.Animation;
import Kurama.geometry.MD5.AnimationFrame;
import Kurama.geometry.MD5.Joint;
import Kurama.geometry.MD5.MD5Utils;
import Kurama.geometry.MeshBuilderHints;
import Kurama.model.AnimatedModel;
import Kurama.utils.Utils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;

import java.util.*;

import static org.lwjgl.assimp.Assimp.*;


public class AssimpAnimLoader2 {

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

        Map<String, Bone> boneList = new HashMap<>();  //Map of bones with inverse bind transforms

        int numMeshes = aiScene.mNumMeshes();
        PointerBuffer aiMeshes = aiScene.mMeshes();
        List<Mesh> meshes = new ArrayList<>();

        for(int i = 0;i < numMeshes; i++) {
            var aiMesh = AIMesh.create(aiMeshes.get(i));
            var mesh = processMesh(aiMesh, materials, resourcePath, boneList);
            mesh.meshIdentifier = Utils.getUniqueID();
            meshes.add(mesh);
        }

        Node root = buildNodeHierarchy(aiScene.mRootNode(), null);
        Matrix rootGlobalInverse = toMatrix(aiScene.mRootNode().mTransformation()).getInverse();

        Map<String, Animation> animations = new HashMap<>();

        int numAnimations = aiScene.mNumAnimations();
        PointerBuffer aiAnimations = aiScene.mAnimations();

        for(int i = 0;i < numAnimations; i++) {
            AIAnimation aiAnimation = AIAnimation.create(aiAnimations.get(i));
            Animation anim = createAnimation(aiAnimation, root, boneList, rootGlobalInverse);
            animations.put(aiAnimation.mName().dataString(), anim);
        }

        String key1 = (String) animations.keySet().toArray()[0];
        AnimatedModel model = new AnimatedModel(game, meshes, animations, animations.get(key1), Utils.getUniqueID());
        return model;
    }

    public static List<Matrix> getUnbindMatrices(Map<String, Bone> boneMap) throws Exception {
        List<Matrix> results = new ArrayList<>(boneMap.size());

        for(int i = 0;i < boneMap.size();i++) {
            Bone reqBone = null;

            for(var bone: boneMap.values()) {
                if(bone.boneId == i) {
                    reqBone = bone;
                    break;
                }
            }
            if(reqBone == null) {
                throw new Exception("could not find bone with id: "+i);
            }

            results.add(reqBone.unbindMatrix);
        }

        return results;
    }

    public static Animation createAnimation(AIAnimation aiAnimation, Node root, Map<String, Bone> boneMap, Matrix rootGlobalInverse) {
        int numNodes = aiAnimation.mNumChannels();
        var numFrames = AINodeAnim.create(aiAnimation.mChannels().get(0)).mNumPositionKeys();
        Animation finalAnimation = new Animation(aiAnimation.mName().dataString(), new ArrayList<>(), boneMap.size(),
                (float) (numFrames/aiAnimation.mDuration()));

        for(int frameNum = 0; frameNum < numFrames; frameNum++) {
            AnimationFrame frame = new AnimationFrame(numNodes);
            Joint[] joints = new Joint[numNodes];
            recursiveAnimProcess(aiAnimation, joints, boneMap, root, frameNum, Matrix.getIdentityMatrix(4), null, rootGlobalInverse);
            frame.joints = Arrays.asList(joints);
            finalAnimation.animationFrames.add(frame);
        }

        return finalAnimation;
    }

    public static void recursiveAnimProcess(AIAnimation aiAnimation, Joint[] joints, Map<String, Bone> boneMap,
                                            Node currentNode, int frameNum, Matrix parentTransform, Bone parentBone, Matrix rootGlobalInverse) {
        Matrix nodeLocalTransform = currentNode.transformation;
        Matrix accTransform = null;

        if(boneMap.containsKey(currentNode.name)) {
            var currentBone = boneMap.get(currentNode.name);
            AINodeAnim animNode = findAIAnimNode(aiAnimation, currentNode.name);
            nodeLocalTransform = buildNodeTransformationMatrix(animNode, frameNum).getTransformationMatrix();  // builds 4x4 matrix from vec3 pos, vec3 scale, quat orient
            accTransform = parentTransform.matMul(nodeLocalTransform);
            Transformation finalTrans = new Transformation(rootGlobalInverse.matMul(accTransform.matMul(currentBone.unbindMatrix)));
//            finalTrans = new Transformation(Matrix.getIdentityMatrix(4));
            int parentId = -1;
            if(parentBone != null) {
                currentBone.parentName = parentBone.boneName;
                parentId = parentBone.boneId;
            }
            parentBone = currentBone;
            var newJoint = new Joint(currentNode.name, parentId, finalTrans.pos, finalTrans.orientation, finalTrans.scale);
            newJoint.currentID = parentBone.boneId;
            joints[currentBone.boneId] = newJoint;
//            frame.joints.add(newJoint);  //-1 simply because the parent ID would never be used later
        }
        else {
            accTransform = parentTransform.matMul(nodeLocalTransform);
        }

        for(var childNode: currentNode.children) {
            recursiveAnimProcess(aiAnimation, joints, boneMap, childNode, frameNum, accTransform, parentBone, rootGlobalInverse);
        }

    }

    private static Transformation buildNodeTransformationMatrix(AINodeAnim aiNodeAnim, int frame) {

        AIVectorKey.Buffer positionKeys = aiNodeAnim.mPositionKeys();
        AIVectorKey.Buffer scalingKeys = aiNodeAnim.mScalingKeys();
        AIQuatKey.Buffer rotationKeys = aiNodeAnim.mRotationKeys();

        AIVectorKey aiVecKey;
        AIVector3D vec;

        Vector pos = null;
        Quaternion orient = null;
        Vector scale = null;

        int numPositions = aiNodeAnim.mNumPositionKeys();
        if (numPositions > 0) {
            aiVecKey = positionKeys.get(Math.min(numPositions - 1, frame));
            vec = aiVecKey.mValue();
            pos = new Vector(vec.x(), vec.y(), vec.z());
        }
        int numRotations = aiNodeAnim.mNumRotationKeys();
        if (numRotations > 0) {
            AIQuatKey quatKey = rotationKeys.get(Math.min(numRotations - 1, frame));
            AIQuaternion aiQuat = quatKey.mValue();
            orient = new Quaternion(aiQuat.w(), aiQuat.x(), aiQuat.y(), aiQuat.z());
        }
        int numScalingKeys = aiNodeAnim.mNumScalingKeys();
        if (numScalingKeys > 0) {
            aiVecKey = scalingKeys.get(Math.min(numScalingKeys - 1, frame));
            vec = aiVecKey.mValue();

            scale = new Vector(vec.x(), vec.y(), vec.z());
        }

        return new Transformation(orient, pos, scale);
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

    public static Node buildNodeHierarchy(AINode aiNode, Node parent) {

        String nodeName = aiNode.mName().dataString();
        var trans = toMatrix(aiNode.mTransformation());
        Node node = new Node(nodeName, parent, trans);

        int numChildren = aiNode.mNumChildren();
        PointerBuffer aiChildren = aiNode.mChildren();
        for(int i = 0;i < numChildren; i++) {
            AINode aiChildNode = AINode.create(aiChildren.get(i));
            Node childNode = buildNodeHierarchy(aiChildNode, node);
            node.children.add(childNode);
        }
        return node;

    }

    public static Mesh processMesh(AIMesh aiMesh, List<Material> materials, String resourcePath, Map<String, Bone> bonesList) {
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

    public static List processJoints(AIMesh aiMesh, Map<String, Bone> bonesList) {

        List<Vector> jointIndices = new ArrayList<>();
        List<Vector> weights = new ArrayList<>();

        Map<Integer, List<VertexWeight>> weightSet = new HashMap<>();
        int numBones = aiMesh.mNumBones();
        PointerBuffer aiBones = aiMesh.mBones();

        for(int i = 0;i < numBones; i++) {
            AIBone aiBone = AIBone.create(aiBones.get(i));

            int id = bonesList.size();
            var boneName = aiBone.mName().dataString();
            Bone bone;
            if(bonesList.containsKey(boneName)) {
                bone = bonesList.get(boneName);
            }
            else {
                bone = new Bone(id, boneName, toMatrix(aiBone.mOffsetMatrix()));
                bonesList.put(boneName, bone);
            }

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

    public static Matrix toMatrix(AIMatrix4x4 input) {
        float[][] mat = new float[4][4];

        mat[0][0] = input.a1();
        mat[0][1] = input.a2();
        mat[0][2] = input.a3();
        mat[0][3] = input.a4();

        mat[1][0] = input.b1();
        mat[1][1] = input.b2();
        mat[1][2] = input.b3();
        mat[1][3] = input.b4();

        mat[2][0] = input.c1();
        mat[2][1] = input.c2();
        mat[2][2] = input.c3();
        mat[2][3] = input.c4();

        mat[3][0] = input.d1();
        mat[3][1] = input.d2();
        mat[3][2] = input.d3();
        mat[3][3] = input.d4();

        return new Matrix(mat);
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
