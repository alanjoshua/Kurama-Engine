#version 460

in vec2 outTexCoord;
in vec3 mvPos;
layout(location = 0) out vec4 fragColor;

uniform sampler2D texture_sampler;
uniform vec4 ambientLight;

void main()
{
    fragColor = ambientLight * texture(texture_sampler, outTexCoord);
}