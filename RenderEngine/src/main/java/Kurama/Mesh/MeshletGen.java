package Kurama.Mesh;

import Kurama.Math.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MeshletGen {

    public static class BoundValues {
        public BoundValues(){}
        public float minx = Float.POSITIVE_INFINITY, maxx = Float.NEGATIVE_INFINITY,
                miny = Float.POSITIVE_INFINITY,maxy = Float.NEGATIVE_INFINITY,
                minz = Float.POSITIVE_INFINITY, maxz = Float.NEGATIVE_INFINITY;
    }

    public record PosWrapper(Vector v, int prevIndex, BoundValues boundValues) implements Comparable {
        @Override
        public int compareTo(Object o) {
            var v1 = (Vector)v;
            var v2 = ((PosWrapper)o).v;

            var morton1 = getMortonCode((long)(v1.get(0) - boundValues.minx), (long)(v1.get(1) - boundValues.miny),(long)(v1.get(2) - boundValues.minz));
            var morton2 = getMortonCode((long)(v2.get(0) - boundValues.minx), (long)(v2.get(1) - boundValues.miny),(long)(v2.get(2) - boundValues.minz));

            if(morton1 < morton2)
                return -1;
            else if(morton1 == morton2)
                return 0;
            else {
                return 1;
            }
        }
    }

    public static List<Meshlet> sortMeshVertices(Mesh mesh, int numVertsPerPrimitives, int maxPrimitivesPerMeshlet, int maxVertices) {

        var sorted = new ArrayList<PosWrapper>();
        var boundValues = new BoundValues();
        var verts = mesh.getVertices();

        // Find range of values to offset and make everything positive
        for(int i = 0; i < verts.size(); i++) {
            var v = verts.get(i);
            sorted.add(new PosWrapper(v, i, boundValues));

            if (v.get(0) > boundValues.maxx)
                boundValues.maxx = v.get(0);
            if (v.get(1) > boundValues.maxy)
                boundValues.maxy = v.get(1);
            if (v.get(2) > boundValues.maxz)
                boundValues.maxz = v.get(2);

            if (v.get(0) < boundValues.minx)
                boundValues.minx = v.get(0);
            if (v.get(1) < boundValues.miny)
                boundValues.miny = v.get(1);
            if (v.get(2) < boundValues.minz)
                boundValues.minz = v.get(2);
        }

        Collections.sort(sorted);

        var newAttribs = new HashMap<Mesh.VERTATTRIB, List<Vector>>();
        var indicesMapping = new HashMap<Integer, Integer>(sorted.size());

        for(var key: mesh.vertAttributes.keySet()) {
            newAttribs.put(key, new ArrayList<>());
        }

        for(int i = 0; i < sorted.size(); i++) {
            var item = sorted.get(i);
            int index = item.prevIndex;
            indicesMapping.put(item.prevIndex, i);

            for(var key: mesh.vertAttributes.keySet()) {
                newAttribs.get(key).add(mesh.getAttributeList(key).get(index));
            }
        }

        var newIndices = new ArrayList<Integer>(mesh.indices.size());
        for(var prevInd: mesh.indices) {
            newIndices.add(indicesMapping.get(prevInd));
        }

        mesh.vertAttributes = newAttribs;
        mesh.indices = newIndices;

        return null;
    }

    // Modified from https://github.com/johnsietsma/InfPoints/blob/master/com.infpoints/Runtime/Morton.cs
    public static long getMortonCode(Long x, Long y, Long z) {
        checkRange(x, y, z);
        return part1By2(z << 2) + part1By2(y << 1) + part1By2(x);
    }

    public static long part1By2(long x) {
        long x64 = x;

        // x = --10 9876 5432 1098 7654 3210
        x64 &= Long.parseLong("001111111111111111111111", 2);

        // x = ---- ---0 9876 ---- ---- ---- ---- ---- ---- ---- ---- 5432 1098 7654 3210
        x64 = (x64 ^ (x64 << 32)) & Long.parseLong("000000011111000000000000000000000000000000001111111111111111", 2);

        // x = ---- ---0 9876 ---- ---- ---- ---- 5432 1098 ---- ---- ---- ---- 7654 3210
        x64 = (x64 ^ (x64 << 16)) & Long.parseLong("000000011111000000000000000011111111000000000000000011111111", 2);

        // x = ---0 ---- ---- 9876 ---- ---- 5432 ---- ---- 1098 ---- ---- 7654 ---- ---- 3210
        x64 = (x64 ^ (x64 << 8)) & Long.parseLong("0001000000001111000000001111000000001111000000001111000000001111", 2);

        // x = ---0 ---- 98-- --76 ---- 54-- --32 ---- 10-- --98 ---- 76-- --54 ---- 32-- --10
        x64 = (x64 ^ (x64 << 4)) & Long.parseLong("0001000011000011000011000011000011000011000011000011000011000011", 2);

        // x = ---1 --0- -9-- 8--7 --6- -5-- 4--3 --1- -0-- 9--8 --7- -6-- 5--4 --3- -2-- 1--0
        x64 = (x64 ^ (x64 << 2)) & Long.parseLong("0001001001001001001001001001001001001001001001001001001001001001", 2);

        return x64;
    }

    private static void checkRange(long x, long y, long z) {
        if(x > Long.MAX_VALUE || y > Long.MAX_VALUE || z > Long.MAX_VALUE)
            throw new IllegalArgumentException("The value of the coordinate should be within 64 bits long in binary");
    }

}
