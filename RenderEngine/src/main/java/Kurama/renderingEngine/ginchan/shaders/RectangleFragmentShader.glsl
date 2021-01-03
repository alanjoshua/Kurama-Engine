#version 460

layout(location = 0) out vec4 outColor;

in PerVertexData {
    vec4 color;
    vec2 tex;
} fragIn;

layout (location = 0) uniform Rectangle{
    mat4 projectionViewMatrix;
    vec4 corners;
    float hasTexture;
}rectangle;

uniform sampler2D texture_sampler;

void main() {

    vec4 finalColor;
    if (rectangle.hasTexture == 0) {
        finalColor = fragIn.color;
    }
    else {
        finalColor = texture(texture_sampler, fragIn.tex);
    }

    outColor = finalColor;

//    outColor = fragIn.color;

}