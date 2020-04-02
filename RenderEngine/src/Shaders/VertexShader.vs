#version 330

layout (location=0) in vec4 position;

uniform mat4 worldMatrix;
uniform mat4 projectionMatrix;

void main() {
    gl_Position = projectionMatrix * worldMatrix * position;
}