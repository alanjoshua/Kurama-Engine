#version 450

layout (set = 0, binding = 0) uniform sampler2DArray samplerView;


layout (location = 0) in vec2 inUV;
layout (location = 0) out vec4 outColor;

void main() {

    bool inside = ((inUV.x >= 0.0) && (inUV.x <= 1.0) && (inUV.y >= 0.0 ) && (inUV.y <= 1.0));

    // red - cyan
    outColor = inside ? vec4(texture(samplerView, vec3(inUV, 0)).x, 0,0,0) : vec4(0.0);
    outColor += inside ? vec4(0, texture(samplerView, vec3(inUV, 1)).yz, 1) : vec4(0.0);

    // magenta-green
//    vec2 image2RBValues = texture(samplerView, vec3(inUV, 1)).xz;
//    outColor = inside ? vec4(0, texture(samplerView, vec3(inUV, 0)).y, 0, 1) : vec4(0.0);
//    outColor += inside ? vec4(image2RBValues.x, 0, image2RBValues.y, 0) : vec4(0.0);

    // amber-blue
//    outColor = inside ? vec4(texture(samplerView, vec3(inUV, 0)).xy, 0, 1) : vec4(0.0);
//    outColor += inside ? vec4(0, 0, texture(samplerView, vec3(inUV, 1)).z, 0) : vec4(0.0);

//    outColor = vec4(1,0,1,1);
}