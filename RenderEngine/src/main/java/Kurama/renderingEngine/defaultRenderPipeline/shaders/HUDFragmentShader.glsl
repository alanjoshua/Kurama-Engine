#version 460

in vec2 outTexCoord;
in vec3 mvPos;
layout(location = 0) out vec4 fragColor;

uniform sampler2D texture_sampler;
uniform vec4 color;


void main()
{
    vec4 tempTex = texture(texture_sampler, outTexCoord);
    fragColor = color * tempTex.rgba;
}