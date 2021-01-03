#version 460

layout(location = 0) out vec4 outColor;

in PerVertexData {
    vec4 color;
    vec2 tex;
} fragIn;

layout (location = 0) uniform Rectangle{
    mat4 projectionViewMatrix;
    vec4 radius;
    vec2 dimensions;
    float hasTexture;
}rectangle;

uniform sampler2D texture_sampler;

void main() {

//    corner method from ThinMatrix' devlop https://www.youtube.com/watch?v=d5ttbNtpgi4&t=12s
    float r1 = rectangle.radius.x;
    float r2 = rectangle.radius.y;
    float r3 = rectangle.radius.z;
    float r4 = rectangle.radius.w;

    vec2 c1 = vec2(r1, rectangle.dimensions.y - r1);
    vec2 c2 = vec2(rectangle.dimensions.x - r2,rectangle.dimensions.y - r2);
    vec2 c3 = vec2(r3,r3);
    vec2 c4 = vec2(rectangle.dimensions.x - r4,r4);

    vec2 coords = fragIn.tex * rectangle.dimensions;

    // in corner 1 (top left)
    if(coords.x < c1.x && coords.y > c1.y) {
        if(length(coords - c1) > r1) {
            discard;
        }
    }

//    Top right corner
    if(coords.x > c2.x && coords.y > c2.y) {
        if(length(coords - c2) > r2) {
            discard;
        }
    }

//    Botton left corner
    if(coords.x < c3.x && coords.y < c3.y) {
        if(length(coords - c3) > r3) {
            discard;
        }
    }

//    Botton right corner
    if(coords.x > c4.x && coords.y < c4.y) {
        if(length(coords - c4) > r4) {
            discard;
        }
    }

    vec4 finalColor;
    if (rectangle.hasTexture == 0) {
        outColor = fragIn.color;
    }
    else {
        outColor = texture(texture_sampler, fragIn.tex);
    }

}