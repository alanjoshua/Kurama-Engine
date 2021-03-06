#version 460

layout (location=0) in vec3 position;
layout (location=1) in vec2 texCoord;
layout (location=2) in vec3 vertexNormal;
layout (location = 6) in float materialIndex;
layout (location=9) in mat4 modelViewMatrix;
layout (location = 13) in vec4 texOff;


out vec2 outTexCoord;

uniform mat4 projectionMatrix;

uniform int numCols;
uniform int numRows;

void main() {

    gl_Position = projectionMatrix * modelViewMatrix * vec4(position,1);

    // Support for texture atlas, update texture coordinates
    float texXOff = texOff.x;
    float texYOff = texOff.y;
    float x = (texCoord.x / numCols + texXOff);
    float y = (texCoord.y / numRows + texYOff);
    vec2 texCoords_local = vec2(x,y);

    outTexCoord = texCoords_local;


}
