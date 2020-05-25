#version 330

layout (location = 0) in vec4 position;
layout (location = 3) in vec4 color;
layout (location = 1) in vec2 texCoord;
layout (location = 2) in vec3 normal;
layout (location = 4) in vec3 tangent;
layout (location = 5) in vec3 biTangent;
layout (location = 6) in float materialIndex;

uniform mat4 modelViewMatrix;
uniform mat4 projectionMatrix;
uniform mat4 modelLightViewMatrix;
uniform mat4 orthoProjectionMatrix;

out vec4 exColor;
out vec2 outTex;
out vec3 vertNormal;
out vec3 vertPos;
out mat4 outModelViewMatrix;
out mat3 TBN;
out vec4 mLightViewVertexPos;
flat out float materialInd;

void main() {
    vec4 tempPos = modelViewMatrix * position;
    gl_Position = projectionMatrix * tempPos;

    exColor = color;
    outTex = texCoord;

    vertNormal = normalize(modelViewMatrix * vec4(normal, 0.0)).xyz;
    vertPos = tempPos.xyz;
    outModelViewMatrix = modelViewMatrix;

    vec3 T = normalize(vec3(modelViewMatrix * vec4(tangent, 0.0)));
    vec3 B = normalize(vec3(modelViewMatrix * vec4(biTangent, 0.0)));
    vec3 N = normalize(vec3(modelViewMatrix * vec4(normal, 0.0)));
    TBN = mat3(T, B, N);

    mLightViewVertexPos = orthoProjectionMatrix * modelLightViewMatrix * position;
    materialInd = materialIndex;

}