package Shaders;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import Math.Matrix;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.opengl.GL20.*;

// This class was blatantly copied from lwjglgamedev gitbook

public class ShaderProgram {

    private final int programID;
    private int vertexShaderID;
    private int fragmentShaderID;
    private final Map<String,Integer> uniforms;

    public ShaderProgram() throws Exception {
        programID = glCreateProgram();
        if(programID == 0) {
            throw new Exception("Could not create Shader");
        }
        uniforms = new HashMap<>();
    }

    public void createUniform(String uniformName) throws Exception {
        int uniformLocation = glGetUniformLocation(programID, uniformName);
        if(uniformLocation < 0) {
            throw new Exception("Could not find uniform: " + uniformName);
        }
        uniforms.put(uniformName,uniformLocation);
    }

    public void setUniforms(String uniformName, Matrix value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            value.setValuesToFloatBuffer(fb);
            glUniformMatrix4fv(uniforms.get(uniformName),false,fb);
        }
    }

    public void createVertexShader(String shaderCode) throws Exception {
        vertexShaderID = createShader(shaderCode,GL_VERTEX_SHADER);
    }

    public void createFragmentShader(String shaderCode) throws Exception {
        fragmentShaderID = createShader(shaderCode,GL_FRAGMENT_SHADER);
    }

    public int createShader(String shaderCode, int shaderType) throws Exception {
        int shaderID = glCreateShader(shaderType);
        if(shaderID == 0) {
            throw new Exception("Error creating shader. Type: " + shaderType);
        }

        glShaderSource(shaderID,shaderCode);
        glCompileShader(shaderID);

        if(glGetShaderi(shaderID,GL_COMPILE_STATUS) == 0) {
            throw new Exception("Error compiling shader code: "+glGetShaderInfoLog(shaderID,1024));
        }

        glAttachShader(programID,shaderID);

        return shaderID;

    }

    public void link() throws Exception {
        glLinkProgram(programID);
        if(glGetProgrami(programID,GL_LINK_STATUS) == 0) {
            throw new Exception("Error linking Shader code: "+ glGetProgramInfoLog(programID,1024));
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
