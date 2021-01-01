#version 460

in vec2 outTexCoord;
in vec3 mvPos;
layout(location = 0) out vec4 fragColor;

uniform sampler2D texture_sampler;
uniform vec4 color;
uniform float near_plane = 0.1f;
uniform float far_plane = 100;


void main()
{
    vec4 tempTex = texture(texture_sampler, outTexCoord);
    fragColor = color * tempTex.rgba;
}