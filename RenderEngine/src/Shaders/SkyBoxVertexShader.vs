#version 330

layout (location = 0) in vec4 position;
layout (location = 1) in vec2 texCoord;
layout (location = 2) in vec3 normal;

uniform mat4 modelViewMatrix;
uniform mat4 projectionMatrix;

out vec2 outTexCoord;

void main()
{
    gl_Position = projectionMatrix * modelViewMatrix * position;
    outTexCoord = texCoord;
}