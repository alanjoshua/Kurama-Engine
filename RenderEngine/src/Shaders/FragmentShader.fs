#version 330

// From LWJGL book
struct Attenuation {
    float constant;
    float linear;
    float exponent;
};

struct PointLight {
    vec3 color;
    vec3 pos; // Light position is assumed to be in view coordinates
    float intensity;
    Attenuation att;
};

struct Material {
    vec4 ambient;
    vec4 diffuse;
    vec4 specular;
    int hasTexture;
    float reflectance;
};

uniform sampler2D texture_sampler;
uniform vec3 ambientLight;
uniform float specularPower;
uniform Material material;
uniform PointLight pointLight;
uniform vec3 camera_pos;

uniform int shouldUseTexture; //Legacy

in vec4 exColor;
in vec2 outTex;
in vec3 vertNormal;
in vec3 vertPos;

vec4 ambientC;
vec4 diffuseC;
vec4 speculrC;

out vec4 fragColor;

void setupColors(Material material, vec2 textCoord) {
    if (material.hasTexture == 1) {
        ambientC = texture(texture_sampler, textCoord);
        diffuseC = ambientC;
        speculrC = ambientC;
    }
    else {
        ambientC = material.ambient;
        diffuseC = material.diffuse;
        speculrC = material.specular;
    }
}

vec4 calculatePointLight(PointLight light, vec3 pos, vec3 normal) {

    vec4 diffuseColor = vec4(0,0,0,0);
    vec4 specColor = vec4(0,0,0,0);

    //Difuse Light
    vec3 lightDir = light.pos - pos;
    vec3 toLight = normalize(lightDir);
    float diffuseFactor = max(dot(normal, toLight),0);
    diffuseColor = diffuseC * vec4(light.color,1) * light.intensity * diffuseFactor;

    //Specular Light
    vec3 cameraDir = normalize(-pos);
    vec3 fromLight = -toLight;
    vec3 reflectedLight = normalize(reflect(fromLight,normal));
    float specularFactor = max(dot(cameraDir, reflectedLight),0);
    specularFactor = pow(specularFactor, specularPower);
    specColor = speculrC * specularFactor * material.reflectance * vec4(light.color, 1);

    //Attenuation
    float distance = length(lightDir);
    float attInv = light.att.constant + light.att.linear * distance + light.att.exponent * distance * distance;
    return (diffuseColor + specColor) / attInv;

}

void main() {
      //if(shouldUseTexture == 1) {
     //     fragColor = texture(texture_sampler,outTex);
     // }
      //else {
     //     fragColor = exColor;
     // }

     setupColors(material, outTex);
     vec4 diffuseSpecularComp = calculatePointLight(pointLight, vertPos, vertNormal);
     fragColor = ambientC * vec4(ambientLight, 1) + diffuseSpecularComp;

}