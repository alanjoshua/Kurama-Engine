package Kurama.Mesh;

// This class is mostly taken as-is from the relevant code in lwjglbook

import java.util.HashMap;
import java.util.Map;

public class TextureCache {

    private static TextureCache INSTANCE;

    private Map<String, Texture> texturesMap;

    private TextureCache() {
        texturesMap = new HashMap<>();
    }

    public static synchronized TextureCache getInstance() {
        if ( INSTANCE == null ) {
            INSTANCE = new TextureCache();
        }
        return INSTANCE;
    }

    public Texture getTexture(String path) throws Exception {
        Texture texture = texturesMap.get(path);
        if ( texture == null ) {
            try {
                texture = new Texture(path);
                texturesMap.put(path, texture);
            }catch (Exception e) {
                return null;
            }
        }
        return texture;
    }
}
