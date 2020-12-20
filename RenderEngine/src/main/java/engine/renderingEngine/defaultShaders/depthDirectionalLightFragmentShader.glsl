#version 460

//uniform int shouldLinearizeDepth;
//uniform float nearZ;
//uniform float farZ;

//float z;

void main() {
    //z = gl_FragCoord.z;
    //if(shouldLinearizeDepth == 1) {

    //}

    gl_FragDepth = gl_FragCoord.z;
}