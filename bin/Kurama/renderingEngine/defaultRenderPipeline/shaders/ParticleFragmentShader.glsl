#version 460

in vec2 outTexCoord;
in vec3 mvPos;

layout(location = 0) out vec4 fragColor;

uniform sampler2D texture_sampler;

void main() {
    fragColor = texture(texture_sampler, outTexCoord);

//    if(gl_FrontFacing) {
//        fragColor = vec4(1,1,1,1);
//    }
//    else {
//        fragColor = fragColor*0.00001 + vec4(1,0,0,1);
//    }

}
