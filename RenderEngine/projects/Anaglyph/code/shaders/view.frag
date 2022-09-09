#version 450

layout (binding = 0) uniform sampler2DArray samplerView;


layout (location = 0) in vec2 inUV;
layout (location = 0) out vec4 outColor;

void main() {

    bool inside = ((inUV.x >= 0.0) && (inUV.x <= 1.0) && (inUV.y >= 0.0 ) && (inUV.y <= 1.0));
    outColor = inside ? texture(samplerView, vec3(inUV, 0)) : vec4(0.0);
    outColor += inside ? texture(samplerView, vec3(inUV, 1)) : vec4(0.0);
}