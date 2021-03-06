package Kurama.shader;

import Kurama.Effects.Fog;
import Kurama.Math.Matrix;
import Kurama.Math.Vector;
import Kurama.Mesh.Material;
import Kurama.lighting.DirectionalLight;
import Kurama.lighting.PointLight;
import Kurama.lighting.SpotLight;
import Kurama.utils.Logger;
import Kurama.utils.Utils;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.NVMeshShader.GL_MESH_SHADER_NV;

// This base of this class was taken from lwjglgamedev gitbook

public class ShaderProgram {

    private final int programID;
    private int vertexShaderID;
    private int fragmentShaderID;
    private int meshShaderID;
    private final Map<String,Integer> uniforms;

    public String shaderIdentifier;

//    public String vertexShaderLocation = null;
//    public String fragmentShaderLocation = null;

    public ShaderProgram(String shaderIdentifier) {
        this.shaderIdentifier = shaderIdentifier;
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
        createUniform(uniformName + ".doesProduceShadow");
    }

    public void createDirectionalLightUniform(String uniformName) {
        createUniform(uniformName + ".color");
        createUniform(uniformName + ".direction");
        createUniform(uniformName + ".intensity");
        createUniform(uniformName + ".doesProduceShadow");
    }

    public void createSpotLightUniform(String uniformName) {
        createPointLightUniform(uniformName + ".pl");
        createUniform(uniformName + ".coneDir");
        createUniform(uniformName + ".cutOff");
        createUniform(uniformName + ".doesProduceShadow");
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

        createUniform(uniformName + ".numRows");
        createUniform(uniformName + ".numCols");
//        createUniform(uniformName + ".texPos");
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
        setUniform(uniformName + ".doesProduceShadow", pointLight.doesProduceShadow ?1:0);
    }

    public void setUniform(String uniformName, DirectionalLight dirLight) {
        setUniform(uniformName + ".color", dirLight.color);
        setUniform(uniformName + ".direction", dirLight.direction_Vector);
        setUniform(uniformName + ".intensity", dirLight.intensity);
        setUniform(uniformName + ".doesProduceShadow", dirLight.doesProduceShadow?1:0);
    }

    public void setUniform(String uniformName, SpotLight spotLight) {
        setUniform(uniformName+".pl",spotLight.pointLight);
        setUniform(uniformName+".coneDir",spotLight.coneDirection);
        setUniform(uniformName+".cutOff",spotLight.cutOff);
        setUniform(uniformName + ".doesProduceShadow", spotLight.doesProduceShadow?1:0);
    }

    public int setMaterials_bindTextures(String uniformName, String textureName, String normalName, String diffuseName, String specularName, List<Material> materials, int off) {
        int offset = off;
        for(int i = 0;i < materials.size();i++) {

            var material = materials.get(i);
            if (material.texture != null) {
                glActiveTexture(offset+GL_TEXTURE0);
                glBindTexture(GL_TEXTURE_2D, material.texture.getId());
            }

            if (material.normalMap != null) {
                glActiveTexture(offset+1+GL_TEXTURE0);
                glBindTexture(GL_TEXTURE_2D, material.normalMap.getId());
            }

            if (material.diffuseMap != null) {
                glActiveTexture(offset+2+GL_TEXTURE0);
                glBindTexture(GL_TEXTURE_2D, material.diffuseMap.getId());
            }

            if (material.specularMap != null) {
                glActiveTexture(offset+3+GL_TEXTURE0);
                glBindTexture(GL_TEXTURE_2D, material.specularMap.getId());
            }

            offset = setUniform(uniformName+"["+i+"]",textureName+"["+i+"]",normalName+"["+i+"]",
                    diffuseName+"["+i+"]",specularName+"["+i+"]",materials.get(i),offset);
        }
        return offset;
    }

    public int setAndActivateDirectionalShadowMaps(String uniformName, List<DirectionalLight> lights, int off) {
        for(int i = 0;i < lights.size();i++) {
            if(lights.get(i).shadowMap != null) {
                glActiveTexture(off + i + GL_TEXTURE0);
                glBindTexture(GL_TEXTURE_2D, lights.get(i).shadowMap.depthMap.getId());
                setUniform(uniformName + "[" + i + "]", off + i);
            }
        }
        return off+lights.size();
    }
    public int setAndActivateSpotLightShadowMaps(String uniformName, List<SpotLight> spotlights, int off) {
        for(int i = 0;i < spotlights.size();i++) {
            if(spotlights.get(i).shadowMap != null) {
                glActiveTexture(off + i + GL_TEXTURE0);
                glBindTexture(GL_TEXTURE_2D, spotlights.get(i).shadowMap.depthMap.getId());
                setUniform(uniformName + "[" + i + "]", off + i);
            }
        }
        return off+spotlights.size();
    }

    public int setUniform(String uniformName, String textureName, String normalName, String diffuseName, String specularName,
                          Material material,int offset) {

        if(material.ambientColor == null) {
            Logger.logError("mat amb null");
        }

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

        setUniform(uniformName+".numCols", material.texture == null ? 1 : material.texture.numCols);
        setUniform(uniformName+".numRows", material.texture == null ? 1 : material.texture.numRows);

        this.setUniform(textureName,offset);
        this.setUniform(normalName,offset+1);
        this.setUniform(diffuseName,offset+2);
        this.setUniform(specularName,offset+3);

        return (offset + 4);
    }

    public void setUniform(String uniformName, Matrix value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            value.setValuesToBuffer(fb);
            fb.flip();
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
            var key = uniforms.get(uniformName);
            var data = value.getData();

            if(key == null) {
                Logger.log("key is null");
            }

            glUniform4fv(key, data);
        }
        else if(value.getNumberOfDimensions() == 2) {
            glUniform2fv(uniforms.get(uniformName), value.getData());
        }
        //Should probably throw an error
        else {
            System.err.println("Cannot set vector uniform. Can only set vectors of size 2, 3 or 4");
        }
    }


    public void createVertexShader(String shaderLocation) throws IOException {
        vertexShaderID = createShader(shaderLocation,GL_VERTEX_SHADER);
    }

    public void createFragmentShader(String shaderLocation) throws IOException {
        fragmentShaderID = createShader(shaderLocation,GL_FRAGMENT_SHADER);
    }

    public void createMeshShader(String shaderLocation) throws IOException {
        meshShaderID = createShader(shaderLocation, GL_MESH_SHADER_NV);
    }

    public int createShader(String shaderLocation, int shaderType) throws IOException {
        int shaderID = glCreateShader(shaderType);
        if(shaderID == 0) {
            throw new RuntimeException("Error creating shader. Type: " + shaderType);
        }

        glShaderSource(shaderID, Utils.loadResourceAsString(shaderLocation));
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
            glDeleteShader(vertexShaderID);
        }

        if(fragmentShaderID != 0) {
            glDetachShader(programID,fragmentShaderID);
            glDeleteShader(fragmentShaderID);
        }

        if(meshShaderID != 0) {
            glDetachShader(programID,meshShaderID);
            glDeleteShader(meshShaderID);
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
