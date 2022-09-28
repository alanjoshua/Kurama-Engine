#version 450

layout (set = 0, binding = 0) uniform sampler2DArray samplerView;


layout (location = 0) in vec2 inUV;
layout (location = 0) out vec4 outColor;

//push constants block
layout( push_constant ) uniform constants
{
    float imageIndex;
} PushConstants;

void main() {

    bool inside = ((inUV.x >= 0.0) && (inUV.x <= 1.0) && (inUV.y >= 0.0 ) && (inUV.y <= 1.0));

    // Temp, probably needs to be updated.
    // Get only red channel from left, and GB channels from right

//     outColor = inside ? vec4(texture(samplerView, vec3(inUV, 0)).x, 0,0,0) : vec4(0.0);
//     outColor += inside ? vec4(0, texture(samplerView, vec3(inUV, 1)).yz, 1) : vec4(0.0);

    if(PushConstants.imageIndex < 1.0f) {
        outColor = inside ? vec4(texture(samplerView, vec3(inUV, 0))) : vec4(0.0);
        //outColor = inside ? vec4(texture(samplerView, vec3(inUV, 0)).x, 0,0,0) : vec4(0.0);
    }
    else {
        outColor = inside ? vec4(texture(samplerView, vec3(inUV, 1))) : vec4(0.0);
        //outColor = inside ? vec4(0, texture(samplerView, vec3(inUV, 1)).yz, 1) : vec4(0.0);
    }

//    outColor = vec4(1,0,1,1);
}