#version 460

layout (location = 0) in vec3 position;
layout (location = 3) in vec4 color;
layout (location = 1) in vec2 texCoord;
layout (location = 2) in vec3 normal;
layout (location = 4) in vec3 tangent;
layout (location = 5) in vec3 biTangent;
layout (location = 6) in float materialIndex;
layout (location = 7) in vec4 biases;
layout (location = 8) in vec4 jointIndices;
layout (location = 9) in mat4 modelLightViewInstancedMatrix;
layout (location = 13) in vec2 texOff;

uniform mat4 modelLightViewMatrix;
uniform mat4 projectionMatrix;
uniform int isAnimated = 0;
uniform int isInstanced;

const int MAX_WEIGHTS = 4;
const int MAX_JOINTS = 150;

uniform mat4 jointMatrices[MAX_JOINTS];

void main() {

    mat4 modelLightViewMatrix_local;
    if(isInstanced > 0) {
        modelLightViewMatrix_local = modelLightViewInstancedMatrix;
    }
    else {
        modelLightViewMatrix_local = modelLightViewMatrix;
    }

    vec4 initPos = vec4(0, 0, 0, 0);

    int count = 0;
    if (isAnimated != 0) {
        for (int i = 0;i < MAX_WEIGHTS;i++) {
            float weight = biases[i];
            if (weight > 0f) {
                count++;
                int jointIndex = int(jointIndices[i]);
                vec4 temp = jointMatrices[jointIndex] * vec4(position, 1.0);
                initPos += weight * temp;
            }
        }
    }

    if (count == 0) {
        initPos = vec4(position, 1.0);
    }

    gl_Position = projectionMatrix * modelLightViewMatrix_local * initPos;
}