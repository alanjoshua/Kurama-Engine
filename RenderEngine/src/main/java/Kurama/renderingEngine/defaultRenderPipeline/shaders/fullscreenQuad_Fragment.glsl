#version 460

in vec2 textureCoord;
uniform sampler2D texture_sampler;
out vec4 fragColor;

void main() {
    fragColor = texture(texture_sampler,textureCoord);
}