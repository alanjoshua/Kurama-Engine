#version 460
#extension GL_ARB_separate_shader_objects : enable

struct CameraData {
    mat4 projview;
    mat4 view;
    mat4 proj;
};

layout(std140, set = 0, binding = 0) uniform CameraBuffer {
   CameraData cameras[2];
}cameraBuffer;

struct ObjectData {
    mat4 model;
};

layout(std140, set = 1, binding = 0) readonly buffer ObjectBuffer {
    ObjectData objects[];
}objectBuffer;

//push constants block
layout( push_constant ) uniform constants
{
    mat4 render_matrix;
    vec4 data;
} PushConstants;

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec2 inTexCoord;
layout(location = 2) in vec3 inNormal;

layout(location = 1) out vec2 fragTexCoord;
layout(location = 2) out vec3 normal;

void main() {

    fragTexCoord = inTexCoord;
    normal = inNormal;

    mat4 transformMatrix = cameraBuffer.cameras[0].projview * objectBuffer.objects[gl_BaseInstance].model;
    gl_Position = transformMatrix * vec4(inPosition, 1.0);
}