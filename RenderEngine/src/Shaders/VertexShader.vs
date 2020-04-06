#version 330

layout (location = 0) in vec4 position;
layout (location = 3) in vec3 texCoord;

uniform mat4 worldMatrix;
uniform mat4 projectionMatrix;

out vec3 outTexCoord;

void main() {
    gl_Position = projectionMatrix * worldMatrix * position;
    outTexCoord = texCoord;
}