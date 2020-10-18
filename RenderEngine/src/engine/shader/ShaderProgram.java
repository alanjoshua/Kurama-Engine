package engine.shader;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import engine.Effects.Fog;
import engine.Effects.ShadowMap;
import engine.Math.Matrix;
import engine.Math.Vector;
import engine.lighting.DirectionalLight;
import engine.Effects.Material;
import engine.lighting.PointLight;
import engine.lighting.SpotLight;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.opengl.GL20.*;

// This class was blatantly copied from lwjglgamedev gitbook

public class ShaderProgram {

    private final int programID;
    private int vertexShaderID;
    private int fragmentShaderID;
    private final Map<String,Integer> uniforms;

    public ShaderProgram() {
        programID = glCreateProgram();
        if(programID == 0) {
            throw new RuntimeException("Could not create Shader");
        }
        uniforms = new HashMap<>();
    }

    public void createUniform(String uniformName) {
        int uniformLocation = glGetUniformLocation(programID, uniformName);
        if(uniformLocation < 0) {
            throw new RuntimeException("Could not find uniform: " + uniformName);
        }
        uniforms.put(uniformName,uniformLocation);
    }

    public void createPointLightUniform(String uniformName) {
        createUniform(uniformName + ".color");
        createUniform(uniformName + ".pos");
        createUniform(uniformName + ".intensity");
        createUniform(uniformName + ".att.constant");
        createUniform(uniformName + ".att.linear");
        createUniform(uniformName + ".att.exponent");
    }

    public void createDirectionalLightUniform(String uniformName) {
        createUniform(uniformName + ".color");
        createUniform(uniformName + ".direction");
        createUniform(uniformName + ".intensity");
    }

    public void createSpotLightUniform(String uniformName) {
        createPointLightUniform(uniformName + ".pl");
        createUniform(uniformName + ".coneDir");
        createUniform(uniformName + ".cutOff");
    }

    public void createMaterialUniform(String uniformName) {
        createUniform(uniformName + ".ambient");
        createUniform(uniformName + ".diffuse");
        createUniform(uniformName + ".specular");
        createUniform(uniformName + ".hasTexture");
        createUniform(uniformName + ".hasNormalMap");
        createUniform(uniformName + ".hasDiffuseMap");
        createUniform(uniformName + ".hasSpecularMap");
        createUniform(uniformName + ".reflectance");
        createUniform(uniformName + ".specularPower");
    }

    public void createMaterialListUniform(String uniformName, String textureName, String normalName, String diffuseName, String specularName,int size) {
        for(int i = 0;i < size;i++) {
            createMaterialUniform(uniformName+"["+i+"]");
            createUniform(textureName+"["+i+"]");
            createUniform(normalName+"["+i+"]");
            createUniform(diffuseName+"["+i+"]");
            createUniform(specularName+"["+i+"]");
            //createUniform(reflectionName+"["+i+"]");
        }
    }

    public void createUniformArray(String uniformName, int size) {
        for(int i = 0;i < size;i++) {
            createUniform(uniformName+"["+i+"]");
        }
    }

    public void createPointLightListUniform(String uniformName, int size) {
        for (int i = 0; i < size; i++) {
            createPointLightUniform(uniformName + "[" + i + "]");
        }
    }

    public void createSpotLightListUniform(String uniformName, int size) {
        for (int i = 0; i < size; i++) {
            createSpotLightUniform(uniformName + "[" + i + "]");
        }
    }

    public void createDirectionalLightListUniform(String uniformName, int size) {
        for (int i = 0; i < size; i++) {
            createDirectionalLightUniform(uniformName + "[" + i + "]");
        }
    }

    public void createFogUniform(String uniformName) {
        createUniform(uniformName+".isActive");
        createUniform(uniformName+".color");
        createUniform(uniformName+".density");
    }

    public void setUniform(String uniformName, PointLight[] pointLights) {
        int numLights = pointLights != null ? pointLights.length : 0;
        for (int i = 0; i < numLights; i++) {
            setUniform(uniformName, pointLights[i], i);
        }
    }

    public void setUniform(String uniformName, PointLight pointLight, int pos) {
        setUniform(uniformName + "[" + pos + "]", pointLight);
    }

    public void setUniform(String uniformName, SpotLight[] spotLights) {
        int numLights = spotLights != null ? spotLights.length : 0;
        for (int i = 0; i < numLights; i++) {
            setUniform(uniformName, spotLights[i], i);
        }
    }

    public void setUniform(String uniformName, SpotLight spotLight, int pos) {
        setUniform(uniformName + "[" + pos + "]", spotLight);
    }

    public void setUniform(String uniformName, DirectionalLight[] directionalLights) {
        int numLights = directionalLights != null ? directionalLights.length : 0;
        for (int i = 0; i < numLights; i++) {
            setUniform(uniformName, directionalLights[i], i);
        }
    }

    public void setUniform(String uniformName, DirectionalLight directionalLight, int pos) {
        setUniform(uniformName+"["+pos+"]",directionalLight);
    }

    public void setUniform(String uniformName, PointLight pointLight) {
        setUniform(uniformName + ".color", pointLight.color);
        setUniform(uniformName + ".pos", pointLight.pos);
        setUniform(uniformName + ".intensity", pointLight.intensity);
        PointLight.Attenuation att = pointLight.attenuation;
        setUniform(uniformName + ".att.constant", att.constant);
        setUniform(uniformName + ".att.linear", att.linear);
        setUniform(uniformName + ".att.exponent", att.exponent);
    }

    public void setUniform(String uniformName, DirectionalLight dirLight) {
        setUniform(uniformName + ".color", dirLight.color);
        setUniform(uniformName + ".direction", dirLight.direction_Vector);
        setUniform(uniformName + ".intensity", dirLight.intensity);
    }

    public void setUniform(String uniformName, SpotLight spotLight) {
        setUniform(uniformName+".pl",spotLight.pointLight);
        setUniform(uniformName+".coneDir",spotLight.coneDirection);
        setUniform(uniformName+".cutOff",spotLight.cutOff);
    }

    public int setAndActivateMaterials(String uniformName, String textureName, String normalName, String diffuseName, String specularName, List<Material> materials, int off) {
        int offset = off;
        for(int i = 0;i < materials.size();i++) {
            offset = setUniform(uniformName+"["+i+"]",textureName+"["+i+"]",normalName+"["+i+"]",diffuseName+"["+i+"]",specularName+"["+i+"]",materials.get(i),offset);
        }
        return offset;
    }

    public int setAndActivateDirectionalShadowMaps(String uniformName, List<DirectionalLight> shadowMaps, int off) {
        for(int i = 0;i < shadowMaps.size();i++) {
            glActiveTexture(off+i+GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D,shadowMaps.get(i).shadowMap.depthMap.getId());
            setUniform(uniformName+"["+i+"]",off+i);
        }
        return off+shadowMaps.size();
    }
    public int setAndActivateSpotLightShadowMaps(String uniformName, List<SpotLight> shadowMaps, int off) {
        for(int i = 0;i < shadowMaps.size();i++) {
            glActiveTexture(off+i+GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D,shadowMaps.get(i).shadowMap.depthMap.getId());
            setUniform(uniformName+"["+i+"]",off+i);
        }
        return off+shadowMaps.size();
    }

    public int setUniform(String uniformName, String textureName, String normalName, String diffuseName, String specularName, Material material,int offset) {
        setUniform(uniformName + ".ambient", material.ambientColor);
        setUniform(uniformName + ".diffuse", material.diffuseColor);
        setUniform(uniformName + ".specular", material.specularColor);
        setUniform(uniformName + ".hasTexture", material.texture == null ? 0 : 1);
        setUniform(uniformName + ".hasNormalMap", material.normalMap == null ? 0 : 1);
        setUniform(uniformName + ".hasDiffuseMap", material.diffuseMap == null ? 0 : 1);
        setUniform(uniformName + ".hasSpecularMap", material.specularMap == null ? 0 : 1);
       // setUniform(uniformName + ".hasReflectionMap", material.reflectionMap == null ? 0 : 1);
        setUniform(uniformName + ".reflectance", material.reflectance);
        setUniform(uniformName+".specularPower",material.specularPower);

        if (material.texture != null) {
            glActiveTexture(offset+GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, material.texture.getId());
        }

        if (material.normalMap != null) {
            glActiveTexture(offset+GL_TEXTURE1);
            glBindTexture(GL_TEXTURE_2D, material.normalMap.getId());
        }

        if (material.diffuseMap != null) {
            glActiveTexture(offset+GL_TEXTURE2);
            glBindTexture(GL_TEXTURE_2D, material.diffuseMap.getId());
        }

        if (material.specularMap != null) {
            glActiveTexture(offset+GL_TEXTURE3);
            glBindTexture(GL_TEXTURE_2D, material.specularMap.getId());
        }
//        if (material.reflectionMap != null) {
//            glActiveTexture(offset+GL_TEXTURE4);
//            glBindTexture(GL_TEXTURE_2D, material.reflectionMap.getId());
//        }

        this.setUniform(textureName,offset);
        this.setUniform(normalName,offset+1);
        this.setUniform(diffuseName,offset+2);
        this.setUniform(specularName,offset+3);
        //this.setUniform(reflectionName,offset+4);

        return (offset + 4);
    }

    public void setUniform(String uniformName, Matrix value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            value.setValuesToFloatBuffer(fb);
            glUniformMatrix4fv(uniforms.get(uniformName),false,fb);
        }
    }

    public void setUniform(String uniformName, Fog fog) {
        setUniform(uniformName+".isActive", fog.active?1:0);
        setUniform(uniformName+".color", fog.color);
        setUniform(uniformName+".density", fog.density);
    }

    public void setUniform(String uniformName,int value) {
        glUniform1i(uniforms.get(uniformName), value);
    }

    public void setUniform(String uniformName,float value) {
        glUniform1f(uniforms.get(uniformName), value);
    }

    public void setUniform(String uniformName,Vector value) {
        if(value.getNumberOfDimensions() == 3) {
            glUniform3f(uniforms.get(uniformName),
                    value.get(0),value.get(1),value.get(2));
        }
        else if(value.getNumberOfDimensions() == 4) {
            glUniform4fv(uniforms.get(uniformName),value.getData());
        }
        //Should probably throw an error
        else {
            System.err.println("Cannot set vector uniform. Can only set vectors of size 3 or 4");
        }
    }


    public void createVertexShader(String shaderCode) {
        vertexShaderID = createShader(shaderCode,GL_VERTEX_SHADER);
    }

    public void createFragmentShader(String shaderCode) {
        fragmentShaderID = createShader(shaderCode,GL_FRAGMENT_SHADER);
    }

    public int createShader(String shaderCode, int shaderType) {
        int shaderID = glCreateShader(shaderType);
        if(shaderID == 0) {
            throw new RuntimeException("Error creating shader. Type: " + shaderType);
        }

        glShaderSource(shaderID,shaderCode);
        glCompileShader(shaderID);

        if(glGetShaderi(shaderID,GL_COMPILE_STATUS) == 0) {
            throw new RuntimeException("Error compiling shader code: "+glGetShaderInfoLog(shaderID,1024));
        }

        glAttachShader(programID,shaderID);

        return shaderID;

    }

    public void link() {
        glLinkProgram(programID);
        if(glGetProgrami(programID,GL_LINK_STATUS) == 0) {
            throw new RuntimeException("Error linking Shader code: "+ glGetProgramInfoLog(programID,1024));
        }

        if(vertexShaderID != 0) {
            glDetachShader(programID,vertexShaderID);
            glDeleteShader(vertexShaderID);   //If program not working properly, delete this line
        }

        if(fragmentShaderID != 0) {
            glDetachShader(programID,fragmentShaderID);
            glDeleteShader(fragmentShaderID);    //If program not working properly, delete this line
        }

        glValidateProgram(programID);
        if(glGetProgrami(programID,GL_VALIDATE_STATUS) == 0) {
            System.err.println("Warning validating Shader code: "+ glGetProgramInfoLog(programID,1024));
        }

    }

    public void bind() {
        glUseProgram(programID);
    }

    public void unbind() {
        glUseProgram(0);
    }

    public void cleanUp() {
        unbind();
        if(programID != 0) {
            glDeleteProgram(programID);
        }
    }

}
