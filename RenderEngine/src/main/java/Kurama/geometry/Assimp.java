package Kurama.geometry;

import Kurama.Math.Vector;
import Kurama.Mesh.Material;
import Kurama.Mesh.Mesh;
import Kurama.Mesh.Texture;
import Kurama.Mesh.TextureCache;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.assimp.Assimp.*;


// This class mostly taken as-is from the relevant chapter in the lwjglbook
public class Assimp {

    public static List<Mesh> load(String resourcePath, String texturesDir) throws Exception {
        return load(resourcePath, texturesDir, aiProcess_JoinIdenticalVertices | aiProcess_Triangulate | aiProcess_FixInfacingNormals);
    }

    public static List<Mesh> load(String resourcePath, String texturesDir, int flags) throws Exception {

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
            var mat = processMaterial(aiMat, texturesDir, fileType);
            materials.add(mat);
        }

        int numMeshes = aiScene.mNumMeshes();
        PointerBuffer aiMeshes = aiScene.mMeshes();
        List<Mesh> meshes = new ArrayList<>();
        for(int i = 0;i < numMeshes; i++) {
            var aiMesh = AIMesh.create(aiMeshes.get(i));
            var mesh = processMesh(aiMesh, materials, resourcePath);
            meshes.add(mesh);
        }

        return meshes;
    }

    public static Mesh processMesh(AIMesh aiMesh, List<Material> materials, String resourcePath) {

        List<List<Vector>> vertAttribs = new ArrayList<>();

        List<Vector> verts = processAttribute(aiMesh.mVertices());
        List<Vector> textures = processTextureCoords(aiMesh.mTextureCoords(0));
        List<Vector> normals = processAttribute(aiMesh.mNormals());
        List<Vector> tangents = processAttribute(aiMesh.mTangents());
        List<Vector> biTangents = processAttribute(aiMesh.mBitangents());
        List<Integer> indices = processIndices(aiMesh);

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

        return newMesh;
    }

    public static List<Integer> processIndices(AIMesh aiMesh) {

        List<Integer> results = new ArrayList<>();

        int numFaces = aiMesh.mNumFaces();
        AIFace.Buffer aiFaces = aiMesh.mFaces();
        for (int i = 0; i < numFaces; i++) {
            AIFace aiFace = aiFaces.get(i);
            IntBuffer buffer = aiFace.mIndices();
            while (buffer.remaining() > 0) {
                results.add(buffer.get());
            }
        }
        return results;
    }

    public static List<Vector> processAttribute(AIVector3D.Buffer buffer) {

        if(buffer == null) {
            return null;
        }

        List<Vector> res = new ArrayList<>();
        while (buffer.remaining() > 0) {
            AIVector3D vec = buffer.get();
            res.add(new Vector(vec.x(), vec.y(), vec.z()));
        }

        if(res.size() > 0) {
            return res;
        }
        else {
            return null;
        }
    }

    public static List<Vector> processTextureCoords(AIVector3D.Buffer buffer) {

        if(buffer == null) {
            return null;
        }

        List<Vector> res = new ArrayList<>();

        while (buffer.remaining() > 0) {
            AIVector3D texCoords = buffer.get();
            res.add(new Vector(new float[]{texCoords.x(), 1-texCoords.y()}));
        }

        if(res.size() > 0) {
            return res;
        }
        else {
            return null;
        }
    }

    public static int getFlags(MeshBuilderHints hints) {
        int finalFlag = aiProcess_FixInfacingNormals;
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

    public static Material processMaterial(AIMaterial aiMaterial, String texturesDir, String fileType) throws Exception {

        AIColor4D colour = AIColor4D.create();
        AIString path = AIString.calloc();

        aiGetMaterialTexture(aiMaterial, aiTextureType_DIFFUSE, 0, path, (IntBuffer) null, null, null, null, null, null);
        String textPath = path.dataString().split(" ")[0];
        Texture texture = null;

        if (textPath != null && textPath.length() > 0) {
            TextureCache textCache = TextureCache.getInstance();
            texture = textCache.getTexture(texturesDir + "/" + textPath);
        }

        aiGetMaterialTexture(aiMaterial, aiTextureType_HEIGHT, 0, path, (IntBuffer) null, null, null, null, null, null);
        textPath = path.dataString().split(" ")[0];
        Texture bumpMap = null;

        if (textPath != null && textPath.length() > 0) {
            TextureCache textCache = TextureCache.getInstance();
            bumpMap = textCache.getTexture(texturesDir + "/" + textPath);
        }

        Vector ambient = Material.DEFAULTAMBIENTCOLOR;
        int result = aiGetMaterialColor(aiMaterial, AI_MATKEY_COLOR_AMBIENT, aiTextureType_NONE, 0, colour);
        if (result == aiReturn_SUCCESS) {
            ambient = new Vector(colour.r(), colour.g(), colour.b(), colour.a());
        }

        Vector diffuse = Material.DEFAULTDIFFUSECOLOR;
        result = aiGetMaterialColor(aiMaterial, AI_MATKEY_COLOR_DIFFUSE, aiTextureType_NONE, 0, colour);
        if (result == aiReturn_SUCCESS) {
            diffuse = new Vector(colour.r(), colour.g(), colour.b(), colour.a());
        }

        Vector specular = Material.DEFAULTSPECULARCOLOR;
        result = aiGetMaterialColor(aiMaterial, AI_MATKEY_COLOR_SPECULAR, aiTextureType_NONE, 0, colour);
        if (result == aiReturn_SUCCESS) {
            specular = new Vector(colour.r(), colour.g(), colour.b(), colour.a());
        }

        Float reflection = null;
        Float specularPower = null;

        // This check is necessary since assimp is not properly loaded shininess and reflection from .mtl files
        if(!fileType.equalsIgnoreCase("obj")) {
            FloatBuffer t = BufferUtils.createFloatBuffer(1);
            IntBuffer size = BufferUtils.createIntBuffer(1);
            result = aiGetMaterialFloatArray(aiMaterial, AI_MATKEY_REFRACTI, aiTextureType_NONE, 0, t, size);
            if (result == aiReturn_SUCCESS) {
                reflection = t.get();
            }

            IntBuffer size2 = BufferUtils.createIntBuffer(1);
            FloatBuffer specBuf = BufferUtils.createFloatBuffer(1);
            result = aiGetMaterialFloatArray(aiMaterial, AI_MATKEY_SHININESS, aiTextureType_NONE, 0, specBuf, size2);
            if (result == aiReturn_SUCCESS) {
                specularPower = specBuf.get();
            }
        }

        Material material = new Material();
        material.texture = texture;
        material.diffuseMap = texture;
        material.normalMap = bumpMap;
        material.ambientColor = ambient;
        material.diffuseColor = diffuse;
        material.specularColor = specular;
        if(reflection != null) {
            material.reflectance = reflection;
        }
        if(specularPower != null) {
            material.specularPower = specularPower;
        }

        return material;
    }

}
