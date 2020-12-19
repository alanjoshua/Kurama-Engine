package engine.geometry;

import engine.Math.Vector;
import engine.Mesh.Face;
import engine.Mesh.Mesh;
import engine.Mesh.Vertex;
import engine.game.Game;
import engine.Terrain.Terrain;

import java.util.ArrayList;
import java.util.List;

public class TerrainUtils {

    public static Terrain createTerrainFromHeightMap(float[][] heightMap, int textInc, Game game, String identifier) {
        List<Vector> positions = new ArrayList<>();
        List<Vector> texCoords = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        List<Face> faces = new ArrayList<>();

        int w = heightMap.length;
        int h = heightMap[0].length;

        float STARTX = -0.5f;
        float STARTZ = -0.5f;

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
                v1.setAttribute(0,Vertex.MATERIAL);
                v2.setAttribute(0,Vertex.MATERIAL);
                v3.setAttribute(0,Vertex.MATERIAL);
                Face f = new Face();


                if (i < w - 1 && j < h - 1) {
                    int leftTop = i * h + j;
                    int leftBottom = (i + 1) * h + j;
                    int rightBottom = (i + 1) * h + j + 1;
                    int rightTop = i * h + j + 1;

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

                    v1.setAttribute(0,Vertex.MATERIAL);
                    v2.setAttribute(0,Vertex.MATERIAL);
                    v3.setAttribute(0,Vertex.MATERIAL);

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

        Mesh resMesh = new Mesh(indices,faces,vertAttribs,null, null, null);
        resMesh = MeshBuilder.generateTangentAndBiTangentVectors(resMesh, null);

//        for(Face f: resMesh.faces) {
//            resMesh.getVertices().get(f.get(0, Vertex.POSITION)).display();
//        }

        return new Terrain(game,resMesh,identifier,w,h,2);
    }

    public static float interpolateHeightFromTriangle(List<Vector> trig, Vector pos) {
        if(trig.size() != 3) {
            System.out.println("trig coords:");
            for(Vector v: trig) {
                v.display();
            }
            throw new RuntimeException("Error, 3 coords were not provided to trivangle height interpolate method");
        }
        Vector pA = trig.get(0);
        Vector pB = trig.get(1);
        Vector pC = trig.get(2);

        float a = (pB.get(1) - pA.get(1)) * (pC.get(2) - pA.get(2)) - (pC.get(1) - pA.get(1)) * (pB.get(2) - pA.get(2));
        float b = (pB.get(2) - pA.get(2)) * (pC.get(0) - pA.get(0)) - (pC.get(2) - pA.get(2)) * (pB.get(0) - pA.get(0));
        float c = (pB.get(0) - pA.get(0)) * (pC.get(1) - pA.get(1)) - (pC.get(0) - pA.get(0)) * (pB.get(1) - pA.get(1));
        float d = -(a * pA.get(0) + b * pA.get(1) + c * pA.get(2));
        // y = (-d -ax -cz) / b
        float y = (-d - a * pos.get(0) - c * pos.get(2)) / b;
        return y;
    }

    public static boolean areVectors2DApproximatelyEqual(Vector v1, Vector v2) {
        Vector a = new Vector(new float[]{(int)v1.get(0),(int)v1.get(2)});
        Vector b = new Vector(new float[]{(int)v2.get(0),(int)v2.get(2)});
		return a.sub(b).getNorm() < 2;
//        return a.equals(b);
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

                    v12 = v2.cross(v1).normalise();
                    v23 = v3.cross(v2).normalise();
                    v34 = v4.cross(v3).normalise();
                    v41 = v1.cross(v4).normalise();

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
