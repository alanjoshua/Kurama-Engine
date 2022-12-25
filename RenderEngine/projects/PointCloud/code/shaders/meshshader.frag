/* Copyright (c) 2021, Sascha Willems
 *
 * SPDX-License-Identifier: MIT
 *
 */

#version 450

//layout (location = 0) in VertexOutput {
//    vec4 color;
//} vertexOutput;

layout(location = 0) out vec4 outFragColor;


void main()
{
//    outFragColor = vertexOutput.color;
    outFragColor = vec4(255,255,255,255);
}