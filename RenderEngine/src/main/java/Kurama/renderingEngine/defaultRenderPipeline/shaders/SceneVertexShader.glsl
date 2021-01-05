#version 460

layout (location = 0) in vec3 position;
layout (location = 1) in vec2 texCoord;
layout (location = 2) in vec3 normal;
layout (location = 3) in vec4 color;
layout (location = 4) in vec3 tangent;
layout (location = 5) in vec3 biTangent;
layout (location = 6) in float materialIndex;
layout (location = 7) in vec4 biases;
layout (location = 8) in vec4 jointIndices;
layout (location = 9) in mat4 modelToWorldInstancedMatrix;
layout (location = 13) in vec4 materialsGlobalLocInstanced;
layout (location = 14) in vec4 materialsAtlasInstanced;

const int MAX_DIRECTIONAL_LIGHTS = 5;
const int MAX_SPOT_LIGHTS = 10;

const int MAX_WEIGHTS = 4;
const int MAX_JOINTS = 150;

uniform mat4 modelToWorldMatrix;
uniform mat4 projectionMatrix;
uniform int isInstanced;
uniform vec4 materialsGlobalLoc;
uniform vec4 materialsAtlas;

struct Material {
    vec4 ambient;
    vec4 diffuse;
    vec4 specular;
    float specularPower;
    float reflectance;
    int hasTexture;
    int hasNormalMap;
    int hasDiffuseMap;
    int hasSpecularMap;
    int numRows;
    int numCols;
};

layout (column_major, std430, binding=0) buffer joindsDataBlock {
    mat4 jointsDataInstanced[];
};

const int MAX_MATERIALS = 50;
uniform Material materials[MAX_MATERIALS];
uniform mat4 worldToDirectionalLightMatrix[MAX_DIRECTIONAL_LIGHTS];
uniform mat4 worldToSpotlightMatrix[MAX_SPOT_LIGHTS];
uniform mat4 worldToCam;

uniform int numDirectionalLights;
uniform int numberOfSpotLights;
uniform mat4 jointMatrices[MAX_JOINTS];
uniform int isAnimated = 0;

out vec4 exColor;
out vec2 outTex;
out vec3 vertNormal;
out vec3 vertPos;
out mat4 outModelViewMatrix;
out mat3 TBN;
out vec4 mLightViewVertexPos[MAX_DIRECTIONAL_LIGHTS];
out vec4 mSpotLightViewVertexPos[MAX_SPOT_LIGHTS];
flat out int materialInd;
flat out int numDirLight;
flat out int numSpotLights;

void main() {
    vec4 initPos = vec4(0, 0, 0, 0);
    vec4 initNormal = vec4(0, 0, 0, 0);
    vec4 initTangent = vec4(0, 0, 0, 0);
    vec4 initBitangent = vec4(0, 0, 0, 0);

    mat4 modelToWorld_local;
    vec4 materialsGlobalLoc_local;
    vec4 materialsAtlas_local;
//    mat4 jointMatrices_local[MAX_JOINTS];

    if(isInstanced > 0) {
        modelToWorld_local = modelToWorldInstancedMatrix;
        materialsGlobalLoc_local = materialsGlobalLocInstanced;
//        materialsAtlas_local = materialsAtlasInstanced;
    }
    else {
        modelToWorld_local = modelToWorldMatrix;
        materialsGlobalLoc_local = materialsGlobalLoc;
//        materialsAtlas_local = materialsAtlas;
    }

//    Calculate texture coordinate for for texture atlas
    int localMatInd = int(materialIndex);
//    int r = localMatInd / 4;
//    int c = localMatInd % 4;
    int globalInd = int(materialsGlobalLoc_local[localMatInd]);

//    int atlasOffset = int(materialsAtlas[localMatInd]);
//
//    Material material = materials[globalInd];
//    int col = atlasOffset % material.numCols;
//    int row = atlasOffset / material.numCols;
//    float texXOff = float(col / material.numCols);
//    float texYOff = float(row / material.numRows);
//    float x = (texCoord.x / material.numCols + texXOff);
//    float y = (texCoord.y / material.numRows + texYOff);
    vec2 texCoords_local = texCoord;

    int count = 0;
    if (isAnimated != 0) {
        for (int i = 0;i < MAX_WEIGHTS;i++) {

            mat4 jointTransMat;
            int jointIndex = int(jointIndices[i]);
            if(isInstanced != 0) {
                jointTransMat = jointsDataInstanced[jointIndex + (gl_InstanceID * MAX_JOINTS)];
            }
            else {
                jointTransMat = jointMatrices[jointIndex];
            }

            float weight = biases[i];
            if (weight > 0f) {
                count++;
                vec4 temp = jointTransMat * vec4(position, 1.0);
                initPos += weight * temp;

                vec4 tempNormal = jointTransMat * vec4(normal, 0.0);
                initNormal += weight * tempNormal;

                vec4 tempTangent = jointTransMat * vec4(tangent, 0.0);
                initTangent += weight * tempTangent;

                vec4 tempBitangent = jointTransMat * vec4(biTangent, 0.0);
                initBitangent += weight * tempBitangent;
            }
        }
    }

    if (count == 0) {
        initPos = vec4(position, 1.0);
        initNormal = vec4(normal, 0.0);
        initTangent = vec4(tangent, 0.0);
        initBitangent = vec4(biTangent, 0.0);
    }

    mat4 modelViewMatrix = worldToCam * modelToWorld_local;
    vec4 tempPos = modelViewMatrix * initPos;
    gl_Position = projectionMatrix * tempPos;

    exColor = color;

    outTex = texCoords_local;

    vertNormal = normalize(modelViewMatrix * initNormal).xyz;
    vertPos = tempPos.xyz;
    outModelViewMatrix = modelViewMatrix;

    vec3 T = normalize(vec3(modelViewMatrix * initTangent));
    vec3 B = normalize(vec3(modelViewMatrix * initBitangent));
    vec3 N = normalize(vec3(modelViewMatrix * initNormal));
    TBN = mat3(T, B, N);

    for(int i = 0;i < numDirectionalLights;i++) {
        mLightViewVertexPos[i] = worldToDirectionalLightMatrix[i] * modelToWorld_local * vec4(position, 1.0);
    }
    for(int i = 0;i < numberOfSpotLights;i++) {
        mSpotLightViewVertexPos[i] = worldToSpotlightMatrix[i] * modelToWorld_local * vec4(position, 1.0);
        mSpotLightViewVertexPos[i] = mSpotLightViewVertexPos[i]/mSpotLightViewVertexPos[i].w;
    }
    materialInd = globalInd;
    numDirLight = numDirectionalLights;
    numSpotLights = numberOfSpotLights;
}