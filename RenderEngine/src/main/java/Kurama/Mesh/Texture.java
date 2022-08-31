package Kurama.Mesh;

//Class taken from lwjgl gitbook

import Kurama.OpenGL.TextureGL;
import Kurama.Vulkan.TextureVK;
import Kurama.game.Game;

import java.nio.ByteBuffer;

public class Texture<T> {

    public int width;
    public int height;

    public T id;
    public String fileName=null;
    public int numRows = 1;
    public int numCols = 1;

    public static Texture createTexture(String fileName) {
        if(Game.GRAPHICS_API == Game.GraphicsApi.OPENGL) {
            return new TextureGL(fileName);
        }
        else {
            return new TextureVK(fileName);
        }
    }

    public static Texture createTexture(String fileName, int numRows, int numCols) {
        if(Game.GRAPHICS_API == Game.GraphicsApi.OPENGL) {
            return new TextureGL(fileName, numRows, numCols);
        }
        else {
            throw new IllegalArgumentException("Vulkan Textures don't yet support this type of texture creation");
        }
    }

    public static Texture createTexture(int id) {
        if(Game.GRAPHICS_API == Game.GraphicsApi.OPENGL) {
            return new TextureGL(id);
        }
        else {
            throw new IllegalArgumentException("Vulkan Textures don't yet support this type of texture creation");
        }
    }

    public static Texture createTexture(String fileName, float multiplier) {
        if(Game.GRAPHICS_API == Game.GraphicsApi.OPENGL) {
            return new TextureGL(fileName, multiplier);
        }
        else {
            throw new IllegalArgumentException("Vulkan Textures don't yet support this type of texture creation");
        }
    }

    public static Texture createTexture(ByteBuffer buf) {
        if(Game.GRAPHICS_API == Game.GraphicsApi.OPENGL) {
            return new TextureGL(buf);
        }
        else {
            throw new RuntimeException("Vulkan textures don't support initialization through bytebuffers yet");
        }
    }

    public T getId() {
        return id;
    }

    public void cleanUp() {

    }

}
