#version 330

layout (location = 0) in vec4 position;
layout (location = 1) in vec3 color;

uniform mat4 worldMatrix;
uniform mat4 projectionMatrix;

out vec3 exColor;

void main() {
    gl_Position = projectionMatrix * worldMatrix * position;
    exColor = color;
}