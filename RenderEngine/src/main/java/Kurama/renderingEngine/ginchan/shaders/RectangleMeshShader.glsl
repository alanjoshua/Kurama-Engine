#version 460
#extension GL_NV_mesh_shader : require

layout(local_size_x=1) in;
layout(triangles, max_vertices=4, max_primitives=2) out;

uniform mat4 projectionViewMatrix;

vec3 quat_rotate(vec4 q, vec3 p) {
    vec3 res;

    vec3 w = vec3(q.y, q.z, q.w);
    float l = length(w);
    float w2 = l * l;

    res = (p * ((q.x * q.x) - w2)) + (w*2*dot(p, w)) + (cross(w, p) * (2 * q.x));
    return res;
}

// Custom vertex output block
layout (location = 0) out PerVertexData{
    vec4 color;
} v_out[];

out uint gl_PrimitiveCountNV;
out uint gl_PrimitiveIndicesNV[];

const vec4 colors[4] = {vec4(1, 0, 0, 0.25), vec4(0, 1, 0, 0.25), vec4(0, 0, 1, 0.25), vec4(0.5, 0.5, 0.5, 0.25)};

void main() {

    vec4 v1 = vec4(-0.5,-0.5,0,1);
    vec4 v2 = vec4(0.5,-0.5,0,1);
    vec4 v3 = vec4(-0.5,0.5,0,1);
    vec4 v4 = vec4(0.5,0.5,0,1);

    gl_MeshVerticesNV[0].gl_Position = projectionViewMatrix*v1;
    gl_MeshVerticesNV[1].gl_Position = projectionViewMatrix*v2;
    gl_MeshVerticesNV[2].gl_Position = projectionViewMatrix*v3;
    gl_MeshVerticesNV[3].gl_Position = projectionViewMatrix*v4;

    v_out[0].color = colors[0];
    v_out[1].color = colors[1];
    v_out[2].color = colors[2];
    v_out[3].color = colors[3];

    gl_PrimitiveIndicesNV[0] = 0;
    gl_PrimitiveIndicesNV[1] = 1;
    gl_PrimitiveIndicesNV[2] = 2;
    gl_PrimitiveIndicesNV[3] = 2;
    gl_PrimitiveIndicesNV[4] = 1;
    gl_PrimitiveIndicesNV[5] = 3;

    gl_PrimitiveCountNV += 2;
}

