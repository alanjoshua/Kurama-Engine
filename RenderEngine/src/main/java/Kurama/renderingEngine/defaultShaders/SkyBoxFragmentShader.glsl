#version 460

in vec2 outTexCoord;
in vec3 mvPos;
out vec4 fragColor;

uniform sampler2D texture_sampler;
uniform vec4 ambientLight;

void main()
{
    fragColor = ambientLight * texture(texture_sampler, outTexCoord);
}