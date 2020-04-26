#version 330

layout (location = 0) in vec4 position;
layout (location = 3) in vec4 color;
layout (location = 1) in vec2 texCoord;
layout (location = 2) in vec3 normal;

uniform mat4 modelViewMatrix;
uniform mat4 projectionMatrix;

out vec4 exColor;
out vec2 outTex;
out vec3 vertNormal;
out vec3 vertPos;

void main() {
    vec4 tempPos = modelViewMatrix * position;
    gl_Position = projectionMatrix * tempPos;

    exColor = color;
    outTex = texCoord;

    vertNormal = normalize(modelViewMatrix * vec4(normal, 0.0)).xyz;
    vertPos = tempPos.xyz;
}