#version 460

in vec2 outTexCoord;
in vec3 mvPos;
out vec4 fragColor;

uniform int shouldGreyScale=0;
uniform sampler2D texture_sampler;
uniform vec4 color;

void main()
{
    if (shouldGreyScale == 0) {
        fragColor = color * texture(texture_sampler, outTexCoord);
    }
    else {
        fragColor = vec4(texture(texture_sampler, outTexCoord).rrr, 1);
    }
}