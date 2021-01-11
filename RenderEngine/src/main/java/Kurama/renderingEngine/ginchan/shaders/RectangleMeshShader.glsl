#version 460
#extension GL_NV_mesh_shader : require

layout(local_size_x=1) in;
layout(triangles, max_vertices=4, max_primitives=2) out;


layout (std140, binding = 0) uniform Rectangle {
    mat4 projectionViewMatrix;
    vec4 radius;
    vec4 color;
    vec4 overlayColor;
    vec2 texUL;
    vec2 texBL;
    vec2 texUR;
    vec2 texBR;
    vec2 dimensions;
    float hasTexture;
} rectangle;

// Custom vertex output block
layout (location = 0) out PerVertexData{
    vec2 tex;
} v_out[];

out uint gl_PrimitiveCountNV;
out uint gl_PrimitiveIndicesNV[];

const vec4 v1 = vec4(-0.5,-0.5,0f,1f);
const vec4 v2 = vec4(0.5,-0.5,0f,1f);
const vec4 v3 = vec4(-0.5,0.5,0f,1f);
const vec4 v4 = vec4(0.5,0.5,0f,1f);

void main() {

    gl_MeshVerticesNV[0].gl_Position = rectangle.projectionViewMatrix*v1;
    gl_MeshVerticesNV[1].gl_Position = rectangle.projectionViewMatrix*v2;
    gl_MeshVerticesNV[2].gl_Position = rectangle.projectionViewMatrix*v3;
    gl_MeshVerticesNV[3].gl_Position = rectangle.projectionViewMatrix*v4;

    v_out[0].tex = rectangle.texBL;
    v_out[1].tex = rectangle.texBR;
    v_out[2].tex = rectangle.texUL;
    v_out[3].tex = rectangle.texUR;

    gl_PrimitiveIndicesNV[0] = 0;
    gl_PrimitiveIndicesNV[1] = 1;
    gl_PrimitiveIndicesNV[2] = 2;
    gl_PrimitiveIndicesNV[3] = 2;
    gl_PrimitiveIndicesNV[4] = 1;
    gl_PrimitiveIndicesNV[5] = 3;

    gl_PrimitiveCountNV = 2;
}

