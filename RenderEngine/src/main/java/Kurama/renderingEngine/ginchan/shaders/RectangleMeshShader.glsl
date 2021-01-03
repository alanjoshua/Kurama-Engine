#version 460
#extension GL_NV_mesh_shader : require

layout(local_size_x=1) in;
layout(triangles, max_vertices=4, max_primitives=2) out;

//uniform mat4 projectionViewMatrix;

//struct Rectangle {
//    sampler2D texture;
//    int hasTexture;
//    mat4 projectionViewMatrix;
//    vec4 corners;
//};

layout (binding = 0) uniform Rectangle {
    mat4 projectionViewMatrix;
    vec4 radius;
    vec2 dimensions;
    float hasTexture;
} rectangle;

// Custom vertex output block
layout (location = 0) out PerVertexData{
    vec4 color;
    vec2 tex;
} v_out[];

out uint gl_PrimitiveCountNV;
out uint gl_PrimitiveIndicesNV[];

const vec4 colors[4] = {vec4(1, 0, 0, 0.25), vec4(0, 1, 0, 0.25), vec4(0, 0, 1, 0.25), vec4(0.5, 0.5, 0.5, 0.25)};
const vec4 v1 = vec4(-0.5,-0.5,0,1);
const vec4 v2 = vec4(0.5,-0.5,0,1);
const vec4 v3 = vec4(-0.5,0.5,0,1);
const vec4 v4 = vec4(0.5,0.5,0,1);

const vec2 t1 = vec2(0,1);
const vec2 t2 = vec2(1,1);
const vec2 t3 = vec2(0,0);
const vec2 t4 = vec2(1,0);

void main() {

    gl_MeshVerticesNV[0].gl_Position = rectangle.projectionViewMatrix*v1;
    gl_MeshVerticesNV[1].gl_Position = rectangle.projectionViewMatrix*v2;
    gl_MeshVerticesNV[2].gl_Position = rectangle.projectionViewMatrix*v3;
    gl_MeshVerticesNV[3].gl_Position = rectangle.projectionViewMatrix*v4;

    v_out[0].color = colors[0];
    v_out[1].color = colors[1];
    v_out[2].color = colors[2];
    v_out[3].color = colors[3];

    v_out[0].tex = t1;
    v_out[1].tex = t2;
    v_out[2].tex = t3;
    v_out[3].tex = t4;

    gl_PrimitiveIndicesNV[0] = 0;
    gl_PrimitiveIndicesNV[1] = 1;
    gl_PrimitiveIndicesNV[2] = 2;
    gl_PrimitiveIndicesNV[3] = 2;
    gl_PrimitiveIndicesNV[4] = 1;
    gl_PrimitiveIndicesNV[5] = 3;

    gl_PrimitiveCountNV += 2;
}

