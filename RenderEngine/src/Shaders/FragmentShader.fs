#version 330

in vec4 exColor;
in vec2 outTex;

out vec4 fragColor;

uniform sampler2D texture_sampler;
uniform int shouldUseTexture;

void main() {
      if(shouldUseTexture == 1) {
          fragColor = texture(texture_sampler,outTex);
      }
      else {
          fragColor = exColor;
          //fragColor = vec4(1,1,1,1);
      }

}