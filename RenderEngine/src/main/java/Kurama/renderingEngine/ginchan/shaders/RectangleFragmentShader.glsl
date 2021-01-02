#version 460

layout(location = 0) out vec4 OutColor;

in PerVertexData {
    vec4 color;
} fragIn;


void main()
{
    OutColor = fragIn.color;
}
