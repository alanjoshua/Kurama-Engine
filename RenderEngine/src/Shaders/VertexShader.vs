#version 330

layout (location=0) in vec3 position;
//layout (location=1) in vec4 inColour;

//out vec4 exColour;

//uniform mat4 worldMatrix;
//uniform mat4 projectionMatrix;

void main() {
    //gl_Position = projectionMatrix * worldMatrix * position;
    //exColour = inColour;

    gl_Position = vec4(position,1.0);
}