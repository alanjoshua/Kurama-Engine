#version 460

in vec2 outTexCoord;
in vec3 mvPos;
out vec4 fragColor;

uniform int shouldGreyScale=0;
uniform int shouldLinearizeDepth=0;
uniform sampler2D texture_sampler;
uniform vec4 color;
uniform float near_plane = 0.1f;
uniform float far_plane = 100;

float LinearizeDepth(float depth) {
    float z = depth * 2.0 - 1.0; // Back to NDC
    return (2.0 * near_plane * far_plane) / (far_plane + near_plane - z * (far_plane - near_plane));
}

void main()
{
    vec4 tempTex = texture(texture_sampler, outTexCoord);

    if (shouldLinearizeDepth == 1) {
        float depthValue = tempTex.r;
        tempTex = vec4(vec3(LinearizeDepth(depthValue) / far_plane), 1.0); // perspective
    }

    if (shouldGreyScale == 0) {
        fragColor = color * tempTex;
    }
    else {
        fragColor = vec4(tempTex.rrr, 1);
    }
}