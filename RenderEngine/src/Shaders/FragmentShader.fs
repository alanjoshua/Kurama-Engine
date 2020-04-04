#version 330

in vec3 outTexCoord;
out vec4 fragColor;

//uniform sampler2D texture_sampler;

void main() {
    //fragColor = texture(texture_sampler,outTexCoord);
    fragColor = vec4(outTexCoord,1);
    //fragColor = vec4(1,1,1,1);
}