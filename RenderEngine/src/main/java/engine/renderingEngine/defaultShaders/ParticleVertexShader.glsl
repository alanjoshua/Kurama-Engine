#version 460

layout (location=0) in vec4 position;
layout (location=1) in vec2 texCoord;
layout (location=2) in vec3 vertexNormal;
layout (location=9) in mat4 modelViewMatrix;
layout (location=13) in vec2 texOffset;

out vec2 outTexCoord;

uniform mat4 projectionMatrix;

uniform int numCols;
uniform int numRows;

void main() {

    gl_Position = projectionMatrix * modelViewMatrix * position;

    // Support for texture atlas, update texture coordinates
    float x = (texCoord.x / numCols + texOffset.x);
    float y = (texCoord.y / numRows + texOffset.y);
    
    outTexCoord = vec2(x, y);

}
