#version 460
#extension GL_ARB_separate_shader_objects : enable

layout(set = 0, binding = 0) uniform CameraBuffer {
    mat4 projview;
    mat4 view;
    mat4 proj;
} cameraData;

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
layout(location = 1) in vec3 inColor;
layout(location = 2) in vec2 inTexCoord;

layout(location = 0) out vec3 fragColor;
layout(location = 1) out vec2 fragTexCoord;

void main() {

    if(gl_BaseInstance == 0) {
        fragColor = vec3(1,0,0);
    }
    else {
        fragColor = vec3(0,1,0);
    }

    fragTexCoord = inTexCoord;

    mat4 transformMatrix = cameraData.projview * objectBuffer.objects[gl_BaseInstance].model;
    gl_Position = transformMatrix * vec4(inPosition, 1.0);
}