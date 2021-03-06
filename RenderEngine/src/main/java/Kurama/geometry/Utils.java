package Kurama.geometry;

import Kurama.ComponentSystem.components.model.Model;
import Kurama.Math.Perlin;
import Kurama.Math.Vector;
import Kurama.Mesh.Face;
import Kurama.Mesh.Vertex;
import Kurama.camera.Camera;
import Kurama.game.Game;
import Kurama.misc_structures.LinkedList.DoublyLinkedList;

import java.util.ArrayList;
import java.util.List;

public class Utils {
    public static float[][] generateRandomHeightMap(int w, int h, int octaves, float persistence, float roughness, long seed) {
        float[][] heightMap = new float[w][h];
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                heightMap[i][j] = (float) ((Perlin.octavePerlin(i * roughness, seed, j * roughness, octaves, persistence)) * 2f - 1f);
            }
        }
        return heightMap;
    }

    public static float edge(Vector v1, Vector v2, Vector p) {
        return (p.get(0) - v1.get(0)) * (v2.get(1) - v1.get(1)) - (p.get(1) - v1.get(1)) * (v2.get(0) - v1.get(0));
    }

    public static boolean isEar(Vertex v0, Vertex v1, Vertex v2, DoublyLinkedList<Vertex> reflex, List<Vector> vertList) {
        boolean isEar = true;
        reflex.resetLoc();
        for(int j = 0;j < reflex.getSize();j++) {
            Vertex p = reflex.peekNext();
            if(isPointInsideTriangle(v0,v1,v2,p,vertList)) {
                isEar = false;
                break;
            }
        }
        return isEar;
    }

    public static boolean isVertexConvex(Vertex v0, Vertex v1, Vertex v2, List<Vector> vertList) {
        Vector vert0 = vertList.get(v0.getAttribute(Vertex.POSITION));
        Vector vert1 = vertList.get(v1.getAttribute(Vertex.POSITION));
        Vector vert2 = vertList.get(v2.getAttribute(Vertex.POSITION));

        float angle = (vert0.sub(vert1)).getAngleBetweenVectors(vert2.sub(vert1));
        if (angle < 0) {
            angle = 180 - angle;
        }
        return angle <= 180;
    }

    //  Basic area implementation from https://www.geeksforgeeks.org/check-whether-given-point-lies-inside-rectangle-not/
    public static boolean isPointInsideRectangle2D(Vector p, Vector tl, Vector tr, Vector bl, Vector br) {

        float totalArea = areaOfTriangle(tl, bl, br) + areaOfTriangle(tl, br, tr);  // rounding to int since area's do not match due to very minuscule difference in decimal places

        float areaA = areaOfTriangle(p, tl, bl);
        float areaB = areaOfTriangle(p, bl, br);
        float areaC = areaOfTriangle(p, br, tr);
        float areaD = areaOfTriangle(p, tl, tr);

        float totalDecompositionSum = areaA + areaB + areaC + areaD;
        float diff = Math.abs(totalArea - totalDecompositionSum);
        float smaller = Math.min(totalArea, totalDecompositionSum);

        return diff < 0.01f*smaller;  //approximation
    }

    // Implementation from https://www.geeksforgeeks.org/check-whether-given-point-lies-inside-rectangle-not/
    public static float areaOfTriangle(Vector a, Vector b, Vector c) {
        return (float)Math.abs((a.get(0) * (b.get(1) - c.get(1)) +
                b.get(0) * (c.get(1) - a.get(1)) + c.get(0) * (a.get(1) - b.get(1))) / 2.0);
    }

    public static boolean isPointInsideTriangle(Vertex v00, Vertex v11, Vertex v22, Vertex pp,List<Vector> vertices) {
        Vector v0 = vertices.get(v00.getAttribute(Vertex.POSITION));
        Vector v1 = vertices.get(v11.getAttribute(Vertex.POSITION));
        Vector v2 = vertices.get(v22.getAttribute(Vertex.POSITION));
        Vector p = vertices.get(pp.getAttribute(Vertex.POSITION));

        Vector e1 = v0.sub(v1);
        Vector e2 = v2.sub(v1);

        Vector proj1 = e1.normalise().scalarMul(e1.normalise().dot(p));
        Vector proj2 = e2.normalise().scalarMul(e2.normalise().dot(p));
        Vector p_ = proj1.add(proj2);

        float pa = (v0.sub(p_)).getNorm();
        float pb = (v1.sub(p_)).getNorm();
        float pc = (v2.sub(p_)).getNorm();
        float totalArea = e1.getNorm() * e2.getNorm() / 2.0f;

        float alpha = pa * pb / (2.0f * totalArea);
        float beta = pb * pc / (2.0f * totalArea);
        float gamma = 1 - alpha - beta;

        return alpha >= 0 && alpha <= 1 && beta >= 0 && beta <= 1 && alpha + beta + gamma == 1;

    }

    public static boolean isPointInsideTriangle(Vector v0, Vector v1, Vector v2, Vector p) {
        Vector e1 = v0.sub(v1);
        Vector e2 = v2.sub(v1);

        Vector proj1 = e1.normalise().scalarMul(e1.normalise().dot(p));
        Vector proj2 = e2.normalise().scalarMul(e2.normalise().dot(p));
        Vector p_ = proj1.add(proj2);

        float pa = (v0.sub(p_)).getNorm();
        float pb = (v1.sub(p_)).getNorm();
        float pc = (v2.sub(p_)).getNorm();
        float totalArea = e1.getNorm() * e2.getNorm() / 2.0f;

        float alpha = pa * pb / (2.0f * totalArea);
        float beta = pb * pc / (2.0f * totalArea);
        float gamma = 1 - alpha - beta;

        return alpha >= 0 && alpha <= 1 && beta >= 0 && beta <= 1 && alpha + beta + gamma == 1;

    }

    //	from the paper "Generalized Barycentric Coordinates on Irregular Polygons"
        public static float cotangent(Vector a, Vector b, Vector c) {
            Vector ba = a.sub(b);
            Vector bc = c.sub(b);
            return (bc.dot(ba) / (bc.cross(ba)).getNorm());
        }

    public static List<Vector> getBoundingBox(Face f, Camera camera, List<Vector> projectedVectors, Game game) {

        Vector bbMax = new Vector(new float[]{Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY});
        Vector bbMin = new Vector(new float[]{Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY});

        for(Vertex v: f.vertices) {
            Vector curr = projectedVectors.get(v.getAttribute(Vertex.POSITION));
            if(curr.get(2) > 0) {
                if (curr.get(0) < bbMin.get(0)) {
                    bbMin.setDataElement(0, curr.get(0));
                }
                if (curr.get(1) < bbMin.get(1)) {
                    bbMin.setDataElement(1, curr.get(1));
                }
                if (curr.get(0) > bbMax.get(0)) {
                    bbMax.setDataElement(0, curr.get(0));
                }
                if (curr.get(1) > bbMax.get(1)) {
                    bbMax.setDataElement(1, curr.get(1));
                }
            }
        }

        int xMin = (int) Math.max(0, Math.min(camera.getImageWidth() - 1, Math.floor(bbMin.get(0))));
        int yMin = (int) Math.max(0, Math.min(camera.getImageHeight() - 1, Math.floor(bbMin.get(1))));
        int xMax = (int) Math.max(0, Math.min(camera.getImageWidth() - 1, Math.floor(bbMax.get(0))));
        int yMax = (int) Math.max(0, Math.min(camera.getImageHeight() - 1, Math.floor(bbMax.get(1))));

        Vector min = new Vector(new float[]{xMin,yMin});
        Vector max = new Vector(new float[]{xMax,yMax});
        List<Vector> res = new ArrayList<>();
        res.add(min);
        res.add(max);

        return res;

    }

    public static Vector[] getWorldBoundingBox(List<Model> models) {
        float[] dataMin = new float[3];
        dataMin[0] = Float.POSITIVE_INFINITY;
        dataMin[1] = Float.POSITIVE_INFINITY;
        dataMin[2] = Float.POSITIVE_INFINITY;

        float[] dataMax = new float[3];
        dataMax[0] = Float.NEGATIVE_INFINITY;
        dataMax[1] = Float.NEGATIVE_INFINITY;
        dataMax[2] = Float.NEGATIVE_INFINITY;

        for (Model m : models) {
            for (Vector vv : m.meshes.get(0).getVertices()) {

                Vector temp = new Vector(new float[] {vv.get(0),vv.get(1),vv.get(2)});
                Vector v = (temp.mul(m.getScale())).add(m.getPos());

                if (v.get(0) < dataMin[0]) {
                    dataMin[0] = v.get(0);
                }
                if (v.get(1) < dataMin[1]) {
                    dataMin[1] = v.get(1);
                }
                if (v.get(2) < dataMin[2]) {
                    dataMin[2] = v.get(2);
                }

                if (v.get(0) > dataMax[0]) {
                    dataMax[0] = v.get(0);
                }
                if (v.get(1) > dataMax[1]) {
                    dataMax[1] = v.get(1);
                }
                if (v.get(2) > dataMax[2]) {
                    dataMax[2] = v.get(2);
                }
            }
        }
        Vector min = new Vector(dataMin);
        Vector max = new Vector(dataMax);
        Vector[] res = new Vector[2];
        res[0] = min;
        res[1] = max;
        return res;
    }

    public static boolean areVectors2DApproximatelyEqual(Vector v1, Vector v2) {
        Vector a = new Vector(new float[]{(int)v1.get(0),(int)v1.get(2)});
        Vector b = new Vector(new float[]{(int)v2.get(0),(int)v2.get(2)});
//		return a.sub(b).getNorm() < 2;
        return a.equals(b);
    }
}
