#version 330

in vec3 exColor;
in vec2 outTex;

out vec4 fragColor;

uniform sampler2D texture_sampler;
uniform int shouldUseTexture;

void main() {
      if(shouldUseTexture == 1) {
          fragColor = texture(texture_sampler,outTex);
      }
      else {
          fragColor = vec4(exColor,1);
      }

}