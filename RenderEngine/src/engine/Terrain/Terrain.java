package engine.Terrain;

import engine.Math.Perlin;

public class Terrain {

    public static float[][] generateRandomHeightMap(int w, int h, int octaves, float persistance) {
        float[][] heightMap = new float[w][h];
        for(int i = 0;i < w;i++) {
            for (int j = 0; j < h; j++) {
                heightMap[i][j] = (float) ((Perlin.octavePerlin((float)i / (float)w, 0, (float)j / (float)h, octaves, persistance))*2f - 1f);
            }
        }
        return heightMap;
    }

}
