#version 460
#extension GL_NV_mesh_shader : require

layout(local_size_x=1) in;
layout(triangles, max_vertices=4, max_primitives=2) out;

// Custom vertex output block
layout (location = 0) out PerVertexData{
    vec4 color;
} v_out[];  // [max_vertices]

out uint gl_PrimitiveCountNV;
out uint gl_PrimitiveIndicesNV[];

void main()
{
    gl_MeshVerticesNV[0].gl_Position = vec4(-1.0, -1.0, 0.0, 1.0); // Upper Left
    gl_MeshVerticesNV[1].gl_Position = vec4( 1.0, -1.0, 0.0, 1.0); // Upper Right
    gl_MeshVerticesNV[2].gl_Position = vec4(-1.0,  1.0, 0.0, 1.0); // Bottom Left
    gl_MeshVerticesNV[3].gl_Position = vec4( 1.0,  1.0, 0.0, 1.0); // Bottom Right

    v_out[0].color = vec4(1,0,0,0.5);
    v_out[1].color = vec4(0,1,0,0.5);
    v_out[2].color = vec4(0,0,1,0.5);
    v_out[3].color = vec4(0.5,0.5,0.5,0.5);

    gl_PrimitiveIndicesNV[0] = 0;
    gl_PrimitiveIndicesNV[1] = 1;
    gl_PrimitiveIndicesNV[2] = 2;
    gl_PrimitiveIndicesNV[3] = 2;
    gl_PrimitiveIndicesNV[4] = 1;
    gl_PrimitiveIndicesNV[5] = 3;

    gl_PrimitiveCountNV += 2;
}

