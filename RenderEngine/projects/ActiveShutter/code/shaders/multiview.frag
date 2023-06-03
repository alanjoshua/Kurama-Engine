//glsl version 4.5
#version 460

//shader input
layout(location = 1) in vec2 fragTexCoord;
layout(location = 2) in vec3 normal;

//output write
layout (location = 0) out vec4 outFragColor;

layout(set = 0, binding = 1) uniform SceneData {
    vec4 fogColor; // w is for exponent
    vec4 fogDistances; //x for min, y for max, zw unused.
    vec4 ambientColor;
    vec4 sunlightDirection; //w for sun power
    vec4 sunlightColor;
} sceneData;

layout(set = 2, binding = 0) uniform sampler2D tex1;

//uint Part1By2_32(uint x)
//{
//    x &= uint(1023);  // x = ---- ---- ---- ---- ---- --98 7654 3210
//    x = (x ^ (x << uint(16))) & uint(4278190335);  // x = ---- --98 ---- ---- ---- ---- 7654 3210
//    x = (x ^ (x << 8)) &  uint(50393103);  // x = ---- --98 ---- ---- 7654 ---- ---- 3210
//    x = (x ^ (x << 4)) &  uint(51130563);  // x = ---- --98 ---- 76-- --54 ---- 32-- --10
//    x = (x ^ (x << 2)) &  uint(153391689);  // x = ---- 9--8 --7- -6-- 5--4 --3- -2-- 1--0
//    return x;
//}
//
//uint encodeMorton32(uvec3 coordinate)
//{
//    return (uint(Part1By2_32(coordinate.z)) << uint(2)) + (uint(Part1By2_32(coordinate.y)) << uint(1)) + Part1By2_32(coordinate.x);
//}

void main()
{
    outFragColor = texture(tex1, fragTexCoord);

//    uvec3 scaledTexCoord = uvec3(fragTexCoord * 100, 0);
//    uint result = encodeMorton32(scaledTexCoord) % 16581375;
//
//    uint red = (result >> 16) & uint(0xFF);
//    uint green = (result >> 8) & uint(0xFF);
//    uint blue = result & uint(0xFF);
//
//    outFragColor = vec4(red, green, 0, 255);

//    outFragColor = vec4(texture(tex1, fragTexCoord).xyz, 1.0f);
//     outFragColor = vec4(fragTexCoord.x, fragTexCoord.y, 0.5f, 1.0f);
}