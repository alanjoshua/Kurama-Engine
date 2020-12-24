#version 460

in vec2 outTexCoord;
in vec3 mvPos;

out vec4 fragColor;

uniform sampler2D texture_sampler;

void main() {
    fragColor = texture(texture_sampler, outTexCoord);
//    fragColor = fragColor*0.00001 + vec4(1,1,1,1);
}
