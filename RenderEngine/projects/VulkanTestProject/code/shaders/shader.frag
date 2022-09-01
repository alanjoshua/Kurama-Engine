//glsl version 4.5
#version 460

//shader input
layout(location = 1) in vec2 fragTexCoord;
layout(location = 2) in vec3 normal;

//output write
layout (location = 0) out vec4 outFragColor;

layout(set = 0, binding = 1) uniform  SceneData{
    vec4 fogColor; // w is for exponent
    vec4 fogDistances; //x for min, y for max, zw unused.
    vec4 ambientColor;
    vec4 sunlightDirection; //w for sun power
    vec4 sunlightColor;
} sceneData;

layout(set = 2, binding = 0) uniform sampler2D tex1;


void main()
{
   outFragColor = vec4(texture(tex1, fragTexCoord).xyz, 1.0f);
//     outFragColor = vec4(fragTexCoord.x, fragTexCoord.y, 0.5f, 1.0f);
}