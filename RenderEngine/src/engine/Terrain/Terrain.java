package engine.Terrain;

import engine.DataStructure.Mesh.Face;
import engine.DataStructure.Mesh.Mesh;
import engine.DataStructure.Mesh.Vertex;
import engine.DataStructure.Texture;
import engine.Math.Perlin;
import engine.Math.Vector;
import engine.lighting.Material;
import org.lwjgl.system.CallbackI;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Terrain {

    public static float[][] generateRandomHeightMap(int w, int h, int octaves, float persistence, float roughness, long seed) {
        float[][] heightMap = new float[w][h];
        Random random = new Random();
        for(int i = 0;i < w;i++) {
            for (int j = 0; j < h; j++) {
//                heightMap[i][j] = (float) ((Perlin.octavePerlin((float)i / (float)w, 0, (float)j / (float)h, octaves, persistence))*2f - 1f);
                heightMap[i][j] = (float) ((Perlin.octavePerlin(i*roughness, seed, j*roughness, octaves, persistence))*2f - 1f);
            }
        }
        return heightMap;
    }

    public static Mesh createMeshFromHeightMap(float[][] heightMap,String textureFile, int textInc) {
        List<Vector> positions = new ArrayList<>();
        List<Vector> texCoords = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        List<Face> faces = new ArrayList<>();

        int w = heightMap.length;
        int h = heightMap[0].length;

        float STARTX = -0.5f;
        float STARTZ = -0.5f;

        Texture texture = null;

        try {
            texture = new Texture(textureFile);
        }catch (Exception e) {
            e.printStackTrace();
        }

        float incx = Math.abs(STARTX * 2) / (w - 1);
        float incz = Math.abs(STARTZ * 2) / (h - 1);

        for(int i = 0;i < w;i++) {
            for(int j = 0;j < h;j++) {
                Vector pos = new Vector(new float[]{STARTX + incx*i,heightMap[i][j],STARTZ + incz*j});
                positions.add(pos);

                Vector tex = new Vector(new float[]{(float) textInc * (float) i / (float) w, (float) textInc * (float) j / (float) h});
                texCoords.add(tex);

                Vertex v1 = new Vertex();
                Vertex v2 = new Vertex();
                Vertex v3 = new Vertex();
                Face f = new Face();


                if (i < w - 1 && j < h - 1) {
                    int leftTop = j * w + i;
                    int leftBottom = (j + 1) * w + i;
                    int rightBottom = (j + 1) * w + i + 1;
                    int rightTop = j * w + i + 1;

                    v1.setAttribute(rightTop,Vertex.POSITION);
                    v1.setAttribute(rightTop,Vertex.TEXTURE);
                    v1.setAttribute(rightTop,Vertex.NORMAL);
                    v2.setAttribute(leftBottom,Vertex.POSITION);
                    v2.setAttribute(leftBottom,Vertex.TEXTURE);
                    v2.setAttribute(leftBottom,Vertex.NORMAL);
                    v3.setAttribute(leftTop,Vertex.POSITION);
                    v3.setAttribute(leftTop,Vertex.TEXTURE);
                    v3.setAttribute(leftTop,Vertex.NORMAL);

                    f.addVertex(v1);f.addVertex(v2);f.addVertex(v3);
                    faces.add(f);

                    indices.add(rightTop);
                    indices.add(leftBottom);
                    indices.add(leftTop);

                    v1 = new Vertex();
                    v2 = new Vertex();
                    v3 = new Vertex();

                    v1.setAttribute(rightBottom,Vertex.POSITION);
                    v1.setAttribute(rightBottom,Vertex.TEXTURE);
                    v1.setAttribute(rightBottom,Vertex.NORMAL);
                    v2.setAttribute(leftBottom,Vertex.POSITION);
                    v2.setAttribute(leftBottom,Vertex.TEXTURE);
                    v2.setAttribute(leftBottom,Vertex.NORMAL);
                    v3.setAttribute(rightTop,Vertex.POSITION);
                    v3.setAttribute(rightTop,Vertex.TEXTURE);
                    v3.setAttribute(rightTop,Vertex.NORMAL);


                    f = new Face();
                    f.addVertex(v1);f.addVertex(v2);f.addVertex(v3);
                    faces.add(f);

                    indices.add(rightBottom);
                    indices.add(leftBottom);
                    indices.add(rightTop);
                }

            }
        }

        List<List<Vector>> vertAttribs = new ArrayList<>();
        vertAttribs.add(positions);
        vertAttribs.add(texCoords);
        vertAttribs.add(calcNormals(positions,w,h));

        Mesh resMesh = new Mesh(indices,faces,vertAttribs);
        resMesh.material = new Material(texture,0f);
        return resMesh;
    }

    private static List<Vector> calcNormals(List<Vector> posArr, int width, int height) {
        Vector v0 = new Vector(3,0);
        Vector v1 = new Vector(3,0);
        Vector v2 = new Vector(3,0);
        Vector v3 = new Vector(3,0);
        Vector v4 = new Vector(3,0);
        Vector v12 = new Vector(3,0);
        Vector v23 = new Vector(3,0);
        Vector v34 = new Vector(3,0);
        Vector v41 = new Vector(3,0);

        List<Vector> normals = new ArrayList<>();
        Vector normal;

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (row > 0 && row < height -1 && col > 0 && col < width -1) {
                    int i0 = row*width + col;
                    v0 = posArr.get(i0);

                    int i1 = row*width + (col-1);
                    v1 = posArr.get(i1);
                    v1 = v1.sub(v0);

                    int i2 = (row+1)*width + col;
                    v2 = posArr.get(i2);
                    v2 = v2.sub(v0);

                    int i3 = (row)*width + (col+1);
                    v3 = posArr.get(i3);
                    v3 = v3.sub(v0);

                    int i4 = (row-1)*width + col;
                    v4 = posArr.get(i4);
                    v4 = v4.sub(v0);

                    v12 = v1.cross(v2).normalise();
                    v23 = v2.cross(v3).normalise();
                    v34 = v3.cross(v4).normalise();
                    v41 = v4.cross(v1).normalise();

                    normal = v12.add(v23).add(v34).add(v41).normalise();

                } else {
                    normal = new Vector(new float[]{0,1,0});
                }
                normal = normal.normalise();
                normals.add(normal);
            }
        }

        return normals;
    }

}
