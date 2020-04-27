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

struct DirectionalLight {
    vec3 color;
    vec3 direction;
    float intensity;
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
uniform DirectionalLight directionalLight;
uniform vec3 camera_pos;

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

vec4 calculateLight(vec3 lightColor, float intensity, vec3 pos, vec3 lightDir, vec3 normal) {

    vec4 diffuseColor = vec4(0,0,0,0);
    vec4 specularColor = vec4(0,0,0,0);

    //Diffuse Color
    float diffuseFactor = max(dot(normal, lightDir),0);
    diffuseColor = diffuseC * vec4(lightColor,1) * intensity * diffuseFactor;

    //Specular Color
    vec3 cameraDir = normalize(-pos);
    vec3 fromLightDir = -lightDir;
    vec3 reflectedLight = normalize(reflect(fromLightDir, normal));
    float specularFactor = max(dot(cameraDir, reflectedLight),0);
    specularFactor = pow(specularFactor, specularPower);
    specularColor = speculrC * intensity * specularFactor * material.reflectance * vec4(lightColor,1);

    return diffuseColor + specularColor;

}

vec4 calculatePointLight(PointLight light, vec3 pos, vec3 normal) {
    //Light Color
    vec3 lightDir = light.pos - pos;
    vec3 toLight = normalize(lightDir);
    vec4 lightColor = calculateLight(light.color, light.intensity, pos, toLight,normal);

    //Attenuation
    float distance = length(lightDir);
    float attInv = light.att.constant + light.att.linear * distance + light.att.exponent * distance * distance;
    return lightColor / attInv;
}

vec4 calculateDirectionalLight(DirectionalLight light, vec3 pos, vec3 normal) {
    return calculateLight(light.color, light.intensity, pos, normalize(light.direction),normal);
}

void main() {
     setupColors(material, outTex);

     vec4 dirColor = calculateDirectionalLight(directionalLight, vertPos, vertNormal);
     vec4 pointColor = calculatePointLight(pointLight, vertPos, vertNormal);
     vec4 diffuseSpecularComp = pointColor + dirColor;

    fragColor = (ambientC * vec4(ambientLight, 1)) + diffuseSpecularComp;
}