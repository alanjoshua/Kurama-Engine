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

struct SpotLight {
    PointLight pl;
    float cutOff;
    vec3 coneDir;
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

struct Fog {
    int active;
    vec3 color;
    float density;
};


const int MAX_POINT_LIGHTS = 10;
const int MAX_SPOT_LIGHTS = 10;
const int MAX_DIRECTIONAL_LIGHTS = 10;

uniform sampler2D texture_sampler;
uniform vec3 ambientLight;
uniform float specularPower;
uniform Material material;
uniform PointLight pointLights[MAX_POINT_LIGHTS];
uniform SpotLight spotLights[MAX_SPOT_LIGHTS];
uniform DirectionalLight directionalLights[MAX_DIRECTIONAL_LIGHTS];
uniform vec3 camera_pos;
uniform Fog fog;
uniform vec3 allDirectionalLightStatic;

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
        ambientC = material.ambient  + exColor;
        diffuseC = material.diffuse  + exColor;
        speculrC = material.specular + exColor;
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

vec4 calculateSpotLight(SpotLight light, vec3 pos, vec3 normal) {
     vec3 lightDirection = light.pl.pos - pos;
     vec3 toLightDir  = normalize(lightDirection);
     vec3 fromLightDir  = -toLightDir;
     float spotAlpha = dot(fromLightDir, normalize(light.coneDir));

     vec4 color = vec4(0, 0, 0, 0);

     if ( spotAlpha > light.cutOff ) {
        color = calculatePointLight(light.pl, pos, normal);
        color *= (1.0 - (1.0 - spotAlpha)/(1.0 - light.cutOff));
     }
     return color;
}

vec4 calculateFog(vec3 pos, vec4 color, Fog fog, vec3 ambientColor) {
    vec3 fogColor = fog.color * (ambientLight + allDirectionalLightStatic);
    float dist = length(pos);
    float fogFactor = 1.0 / exp((dist * fog.density) * (dist * fog.density));
    fogFactor = clamp(fogFactor, 0.0, 1.0);

    vec3 resultColor = mix(fogColor, color.xyz, fogFactor);
    return vec4(resultColor, color.w);
}

void main() {
     setupColors(material, outTex);
     vec4 color = vec4(0,0,0,0);

     for(int i = 0;i < MAX_POINT_LIGHTS;i++) {
        if(pointLights[i].intensity > 0) {
            color += calculatePointLight(pointLights[i],vertPos,vertNormal);
        }
     }

     for(int i = 0;i < MAX_SPOT_LIGHTS;i++) {
        if(spotLights[i].pl.intensity > 0) {
            color += calculateSpotLight(spotLights[i],vertPos,vertNormal);
        }
     }

     for(int i = 0;i < MAX_DIRECTIONAL_LIGHTS;i++) {
        if(directionalLights[i].intensity > 0) {
            color += calculateDirectionalLight(directionalLights[i],vertPos,vertNormal);
        }
     }

    // vec4 dirColor = calculateDirectionalLight(directionalLight, vertPos, vertNormal);
    // vec4 pointColor = calculatePointLight(pointLight, vertPos, vertNormal);
    // vec4 spotColor = calculateSpotLight(spotLight, vertPos, vertNormal);
    // vec4 diffuseSpecularComp = pointColor + dirColor + spotColor;

    fragColor = (ambientC * vec4(ambientLight, 1)) + color;
    if(fog.active == 1) {
        fragColor = calculateFog(vertPos, fragColor, fog, ambientLight.xyz);
    }

}