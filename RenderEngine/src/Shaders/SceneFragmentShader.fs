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
    float specularPower;
    int hasTexture;
    float reflectance;
    int hasNormalMap;
    int hasDiffuseMap;
    int hasSpecularMap;
    sampler2D texture;
    sampler2D normalMap;
    sampler2D diffuseMap;
    sampler2D specularMap;
};

struct Fog {
    int active;
    vec3 color;
    float density;
};


const int MAX_POINT_LIGHTS = 10;
const int MAX_SPOT_LIGHTS = 10;
const int MAX_DIRECTIONAL_LIGHTS = 10;

uniform sampler2D shadowMap;
uniform vec3 ambientLight;
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
in mat4 outModelViewMatrix;
in mat3 TBN;
in vec4 mLightViewVertexPos;

vec4 ambientC;
vec4 diffuseC;
vec4 speculrC;
float specPower;

out vec4 fragColor;

void setupColors(Material material, vec2 textCoord) {

    specPower = material.specularPower;

    if (material.hasTexture == 1) {
        ambientC = texture(material.texture, textCoord);
    }
    else {
        ambientC = material.ambient;
    }

    if(material.hasDiffuseMap == 1) {
        diffuseC = texture(material.diffuseMap,textCoord);
    }
    else {
        //material.diffuse = ambientC + vec4(0,0,0,0) * 0.0000001;
        diffuseC =  material.diffuse*0.000001 + ambientC;
    }

     if(material.hasSpecularMap == 1) {

        speculrC = texture(material.specularMap,textCoord);
        specPower = speculrC.w;
        speculrC = vec4(speculrC.xyz,1.0);

     }
     else {
       //speculrC = material.specular  + exColor;
       speculrC = material.specular*0.000001;
     }

   // speculrC = ambientC;
    //speculrC = material.specular + exColor;
}

float calculateShadow(vec4 position) {
     vec3 projCoords = position.xyz;
        // Transform from screen coordinates to texture coordinates
        projCoords = projCoords * 0.5 + 0.5;
        float bias = 0.05;

        float shadowFactor = 1;
        vec2 inc = 1.0 / textureSize(shadowMap, 0);
        for(int row = -1; row <= 1; row++) {
            for(int col = -1; col <= 1; col++) {
                float textDepth = texture(shadowMap, projCoords.xy + vec2(row, col) * inc).r;
                shadowFactor += projCoords.z - bias > textDepth ? 1.0 : 0.0;
            }
        }
        shadowFactor /= 9.0;
        if(projCoords.z > 1.0) {
            shadowFactor = 1.0;
        }
        return (1-shadowFactor);
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
    float specularFactor = max(dot(cameraDir, reflectedLight), 0);
    specularFactor = pow(specularFactor, specPower);
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

vec3 calculateNormal(Material material, vec3 normal, vec2 texCoord) {
    vec3 newNormal = normal;
    if(material.hasNormalMap == 1) {
        newNormal = texture(material.normalMap, texCoord).rgb;
        newNormal = normalize(newNormal * 2 - 1);
        newNormal = normalize(TBN * newNormal);
    }
    return newNormal;
}

void main() {
     setupColors(material, outTex);
     vec4 color = vec4(0,0,0,0);
     vec3 normal = calculateNormal(material, vertNormal, outTex);

     for(int i = 0;i < MAX_POINT_LIGHTS;i++) {
        if(pointLights[i].intensity > 0) {
            color += calculatePointLight(pointLights[i],vertPos,normal);
        }
     }

     for(int i = 0;i < MAX_SPOT_LIGHTS;i++) {
        if(spotLights[i].pl.intensity > 0) {
            color += calculateSpotLight(spotLights[i],vertPos,normal);
        }
     }

     float shadowFactor = calculateShadow(mLightViewVertexPos);
     //shadowFactor = 1;
     vec4 temp = vec4(0,0,0,0);
     for(int i = 0;i < MAX_DIRECTIONAL_LIGHTS;i++) {
        if(directionalLights[i].intensity > 0) {
            color += shadowFactor*calculateDirectionalLight(directionalLights[i],vertPos,normal);
        }
     }
     //color += vec4(temp.xyz*shadowFactor,1);
     //color+=temp;

    // vec4 dirColor = calculateDirectionalLight(directionalLight, vertPos, vertNormal);
    // vec4 pointColor = calculatePointLight(pointLight, vertPos, vertNormal);
    // vec4 spotColor = calculateSpotLight(spotLight, vertPos, vertNormal);
    // vec4 diffuseSpecularComp = pointColor + dirColor + spotColor;


    fragColor = (ambientC * vec4(ambientLight, 1)) + (color);
    if(fog.active == 1) {
        fragColor = calculateFog(vertPos, fragColor, fog, ambientLight.xyz);
    }
    fragColor = clamp(fragColor, 0,1);

}