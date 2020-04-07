#version 330

layout (location = 0) in vec4 position;
layout (location = 3) in vec3 color;
layout (location = 1) in vec2 texCoord;

uniform mat4 worldMatrix;
uniform mat4 projectionMatrix;

out vec3 exColor;
out vec2 outTex;

void main() {
    gl_Position = projectionMatrix * worldMatrix * position;
    exColor = color;
    outTex = texCoord;
}