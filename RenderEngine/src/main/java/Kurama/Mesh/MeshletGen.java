package Kurama.Mesh;

import Kurama.Math.Vector;

import java.util.*;

import static Kurama.Mesh.MeshletGen.MeshletColorMode.PerMeshlet;
import static Kurama.utils.Logger.log;

public class MeshletGen {

    public record SortedVertAttribs(Map<Mesh.VERTATTRIB, List<Vector>> sortedVertAttribs, List<Integer> indices, BoundValues boundValues){}
    public static class BoundValues {
        public BoundValues(){}
        public float minx = Float.POSITIVE_INFINITY, maxx = Float.NEGATIVE_INFINITY,
                miny = Float.POSITIVE_INFINITY,maxy = Float.NEGATIVE_INFINITY,
                minz = Float.POSITIVE_INFINITY, maxz = Float.NEGATIVE_INFINITY;
    }

    public record MeshletGenOutput(List<Meshlet> meshlets, Mesh mesh, List<Integer> vertexIndexBuffer, List<Integer> localIndexBuffer){}

    public record PosWrapper(Vector v, int prevIndex, BoundValues boundValues) implements Comparable {
        @Override
        public int compareTo(Object o) {
            var v1 = (Vector) v;
            var v2 = ((PosWrapper) o).v;

            var morton1 = getMortonCode((long) (v1.get(0) - boundValues.minx), (long) (v1.get(1) - boundValues.miny), (long) (v1.get(2) - boundValues.minz));
            var morton2 = getMortonCode((long) (v2.get(0) - boundValues.minx), (long) (v2.get(1) - boundValues.miny), (long) (v2.get(2) - boundValues.minz));

            if (morton1 < morton2)
                return -1;
            else if (morton1 == morton2)
                return 0;
            else {
                return 1;
            }
        }
    }

    public static MeshletGenOutput generateMeshlets(Mesh mesh, int vertsPerPrimitive, int maxVerts, int maxPrimitives) {
        return generateMeshlets(mesh, vertsPerPrimitive, maxVerts, maxPrimitives, 0, 0, 0);
    }

    record Primitive(Integer... indices){}
    static List<Primitive> convertToPrimitives(List<Integer> indices, int vertsPerPrimitive) {
        if(indices.size() % vertsPerPrimitive != 0)
            throw new IllegalArgumentException("The index array must be divisible by the number of vertices per primitive");

        var results = new ArrayList<Primitive>();

        for(int i = 0; i < indices.size(); i+=vertsPerPrimitive) {
            results.add(new Primitive(indices.subList(i, i + vertsPerPrimitive).toArray(new Integer[0])));
        }

        return results;
    }

    public static MeshletGenOutput generateMeshlets(Mesh mesh, int vertsPerPrimitive, int maxVerts, int maxPrimitives,
                                                    int globalVertsBufferPos, int globalVertsIndexBufferPos, int globalLocalIndexBufferPos) {

        if(maxVerts == 0 || maxPrimitives == 0 || maxVerts < vertsPerPrimitive) {
            throw new IllegalArgumentException("Invalid meshlet generation arguments");
        }

//        var sortedPrimitives    = convertToPrimitives(sortMeshIndices(mesh.getVertices(), mesh.indices, vertsPerPrimitive, null), vertsPerPrimitive);
        var sortedPrimitives    = convertToPrimitives(mesh.indices, vertsPerPrimitive);


        var meshlets = new ArrayList<Meshlet>();
        var vertIndices = new ArrayList<Integer>();
        var primIndices = new ArrayList<Integer>();

        boolean isMaxVertsReached = false;
        boolean isMaxPrimsReached = false;

        var curMeshletVertIndices = new LinkedHashSet<Integer>();
        var curMeshletVertMapping = new HashMap<Integer, Integer>();
        var curMeshletLocalIndices = new ArrayList<Integer>();

        var curMeshlet = new Meshlet();
        curMeshlet.vertexBegin = globalVertsIndexBufferPos;
        curMeshlet.indexBegin = globalLocalIndexBufferPos;

        var curMeshletPos = new Vector(0,0,0);
        var curBounds = new BoundValues();

        int numTimesVertLimitReached = 0;
        int numPrimLimitReached = 0;

        for(int pid = 0; pid < sortedPrimitives.size(); pid++) {
            var p = sortedPrimitives.get(pid);
            List<Integer> indexLocsToBeInserted = null;
            List<Integer> uniqueVertsToBeAdded = null;
            isMaxVertsReached = false;
            isMaxPrimsReached = false;

            // Check whether max prim limit would be reached if the current primitive is added to the current meshlet
            if(curMeshletLocalIndices.size()/vertsPerPrimitive + 1 > maxPrimitives) {
                isMaxPrimsReached = true;
                numPrimLimitReached++;
            }

            // Check whether max vert limit would be reached if the current primitive is added to the current meshlet
            // Check only when max prim limit has not yet been reached
            if(!isMaxPrimsReached) {
                indexLocsToBeInserted = new ArrayList<>();
                uniqueVertsToBeAdded = new ArrayList<>();
                int uniqueVertCount = 0;

                for (var v : p.indices()) {

                    if (!curMeshletVertMapping.containsKey(v + globalVertsBufferPos)) {
                        uniqueVertsToBeAdded.add(v + globalVertsBufferPos);
                        curMeshletVertMapping.put(v + globalVertsBufferPos, curMeshletVertIndices.size() + uniqueVertCount);
                        uniqueVertCount++;
                    }
                    var indToInsert = curMeshletVertMapping.get(v + globalVertsBufferPos);
                    if (indToInsert == null) {
                       throw new RuntimeException("Index to be inserted was null, which shouldnt happen, for vertInd: ");
                    }
                    indexLocsToBeInserted.add(curMeshletVertMapping.get(v + globalVertsBufferPos));
                }

                if (uniqueVertCount + curMeshletVertIndices.size() > maxVerts) {
                    isMaxVertsReached = true;
                    numTimesVertLimitReached++;
                }
            }

            // If either of the limits have been reached, add to meshlet list, and create a new meshlet
            if(isMaxVertsReached || isMaxPrimsReached) {
                curMeshlet.primitiveCount = curMeshletLocalIndices.size()/vertsPerPrimitive;
                curMeshlet.vertexCount = curMeshletVertIndices.size();
                curMeshletPos = curMeshletPos.scalarMul(1f/curMeshletVertIndices.size());
                curMeshlet.pos = curMeshletPos; // temporary
                curMeshlet.boundRadius = calculateBoundRadius(curBounds);

                vertIndices.addAll(curMeshletVertIndices);
                primIndices.addAll(curMeshletLocalIndices);

                meshlets.add(curMeshlet);
                curMeshletVertIndices.clear();
                curMeshletLocalIndices.clear();
                curMeshletVertMapping.clear();
                curMeshletPos = new Vector(0,0,0);
                curBounds = new BoundValues();

                curMeshlet = new Meshlet();
                curMeshlet.vertexBegin = globalVertsIndexBufferPos + vertIndices.size();
                curMeshlet.indexBegin = globalLocalIndexBufferPos + primIndices.size();

                //Add current primitive to new meshlet
                for (int i = 0; i < p.indices().length; i++) {
                    curMeshletVertIndices.add(p.indices[i] + globalVertsBufferPos);
                    curMeshletVertMapping.put(p.indices[i] + globalVertsBufferPos, i);
                    curMeshletLocalIndices.add(i);
                }
                continue;
            }

            else {
                //Else, add current primitive to meshlet
                curMeshletVertIndices.addAll(uniqueVertsToBeAdded);
                curMeshletLocalIndices.addAll(indexLocsToBeInserted);

                // Calculate meshlet pos and bounds
                for(int vertsThisPrim: uniqueVertsToBeAdded) {
                    var v = mesh.getVertices().get(vertsThisPrim - globalVertsBufferPos);
                    curMeshletPos = curMeshletPos.add(v);

                    if (v.get(0) > curBounds.maxx)
                        curBounds.maxx = v.get(0);
                    if (v.get(1) > curBounds.maxy)
                        curBounds.maxy = v.get(1);
                    if (v.get(2) > curBounds.maxz)
                        curBounds.maxz = v.get(2);

                    if (v.get(0) < curBounds.minx)
                        curBounds.minx = v.get(0);
                    if (v.get(1) < curBounds.miny)
                        curBounds.miny = v.get(1);
                    if (v.get(2) < curBounds.minz)
                        curBounds.minz = v.get(2);
                }
            }
        }

        //Add last meshlet
        curMeshlet.primitiveCount = curMeshletLocalIndices.size()/vertsPerPrimitive;
        curMeshlet.vertexCount = curMeshletVertIndices.size();
        curMeshletPos = curMeshletPos.scalarMul(1f/curMeshletVertIndices.size());
        curMeshlet.pos = curMeshletPos;
        curMeshlet.boundRadius = calculateBoundRadius(curBounds);

        vertIndices.addAll(curMeshletVertIndices);
        primIndices.addAll(curMeshletLocalIndices);

        meshlets.add(curMeshlet);

        log("Num of times vert limit reached: " + numTimesVertLimitReached + " prim limit reached: "+ numPrimLimitReached);;

        return new MeshletGenOutput(meshlets, mesh, vertIndices, primIndices);
    }

    public static MeshletGenOutput generateMeshletsTemp(Mesh mesh, int vertsPerPrimitive, int maxVerts, int maxPrimitives,
                                                    int globalVertsBufferPos, int globalVertsIndexBufferPos, int globalLocalIndexBufferPos) {

        if(maxVerts == 0 || maxPrimitives == 0 || maxVerts < vertsPerPrimitive) {
            throw new IllegalArgumentException("Invalid meshlet generation arguments");
        }

        List<Integer> vertexIndices = new ArrayList<>();
        List<Integer> localPrimitiveIndices = new ArrayList<>();
        var meshlets = new ArrayList<Meshlet>();

        log("Num of prims before sort"+ mesh.indices.size()/vertsPerPrimitive);
        var sortedPrimitives = sortMeshIndices(mesh.getVertices(), mesh.indices, vertsPerPrimitive, null);
//        var sortedPrimitives = mesh.indices;
        log("Num of prims "+ sortedPrimitives.size()/vertsPerPrimitive);

        var curMeshlet = new Meshlet();
        curMeshlet.indexBegin = globalLocalIndexBufferPos;
        curMeshlet.vertexBegin = globalVertsIndexBufferPos;

        var curUniqueVerts = new ArrayList<Integer>();
        var curIndexList = new ArrayList<Integer>();
        var curMeshletPos = new Vector(0,0,0);
        var curBounds = new BoundValues();

        int primInd = 0;
        while (primInd < sortedPrimitives.size()/vertsPerPrimitive) {

            // We have reached the limit for the number of primitives a meshlet could have
            // We assume that the number of verts is still within the limit
            // We create a new meshlet here
            if((curIndexList.size() / vertsPerPrimitive) > maxPrimitives) {
                vertexIndices.addAll(curUniqueVerts);
                localPrimitiveIndices.addAll(curIndexList);

                curMeshlet.primitiveCount = curIndexList.size() / vertsPerPrimitive;
                curMeshlet.vertexCount = curUniqueVerts.size();
                curMeshlet.pos = curMeshletPos;
                curMeshlet.boundRadius = calculateBoundRadius(curBounds);
                meshlets.add(curMeshlet);

                curUniqueVerts.clear();
                curIndexList.clear();

                curMeshlet = new Meshlet();
                curMeshlet.indexBegin = globalLocalIndexBufferPos + localPrimitiveIndices.size();
                curMeshlet.vertexBegin = globalVertsIndexBufferPos + vertexIndices.size();
            }

            var lastPrimitiveIndices = new ArrayList<Integer>();
            var uniqueVertsAddedThisPrim = new ArrayList<Integer>();

            for(int i = 0; i < vertsPerPrimitive; i++) {
                int baseInd = primInd * vertsPerPrimitive;
                int curVIndex = sortedPrimitives.get(baseInd + i) + globalVertsBufferPos;
                var localIndex = curUniqueVerts.indexOf(curVIndex);

                if(localIndex == -1) {
                    curUniqueVerts.add(curVIndex);
                    uniqueVertsAddedThisPrim.add(curVIndex);
                    localIndex = curUniqueVerts.size() - 1;
                }
                lastPrimitiveIndices.add(localIndex);
            }

            // If max vert limit has not been reached, and by definition max prim has also not been reached
            if(curUniqueVerts.size() <= maxVerts) {
                // add the primitive to the primitive index list
                curIndexList.addAll(lastPrimitiveIndices);
                lastPrimitiveIndices.clear();

                // Calculate meshlet pos and bounds
                for(int vertsThisPrim: uniqueVertsAddedThisPrim) {
                    var v = mesh.getVertices().get(vertsThisPrim - globalVertsBufferPos);
                    curMeshletPos = curMeshletPos.add(v);

                    if (v.get(0) > curBounds.maxx)
                        curBounds.maxx = v.get(0);
                    if (v.get(1) > curBounds.maxy)
                        curBounds.maxy = v.get(1);
                    if (v.get(2) > curBounds.maxz)
                        curBounds.maxz = v.get(2);

                    if (v.get(0) < curBounds.minx)
                        curBounds.minx = v.get(0);
                    if (v.get(1) < curBounds.miny)
                        curBounds.miny = v.get(1);
                    if (v.get(2) < curBounds.minz)
                        curBounds.minz = v.get(2);
                }

                // increment to the next primitive
                primInd++;
            }

            // If max vertex per meshlet limit is reached, create a new meshlet
            else {
                // Remove the verts that were added during this primitive
                curUniqueVerts.removeAll(uniqueVertsAddedThisPrim);

                vertexIndices.addAll(curUniqueVerts);
                localPrimitiveIndices.addAll(curIndexList);

                curMeshlet.primitiveCount = curIndexList.size() / vertsPerPrimitive;
                curMeshlet.vertexCount = curUniqueVerts.size();
                curMeshlet.pos = curMeshletPos;
                curMeshlet.boundRadius = calculateBoundRadius(curBounds);
                meshlets.add(curMeshlet);

                curUniqueVerts.clear();
                curIndexList.clear();

                curMeshlet = new Meshlet();
                curMeshlet.indexBegin = globalLocalIndexBufferPos + localPrimitiveIndices.size();
                curMeshlet.vertexBegin = globalVertsIndexBufferPos + vertexIndices.size();

                // Do not increment to next primitive here so that it could be added to the next meshlet
            }
        }

        // Add last meshlet
        vertexIndices.addAll(curUniqueVerts);
        localPrimitiveIndices.addAll(curIndexList);

        curMeshlet.primitiveCount = curIndexList.size() / vertsPerPrimitive;
        curMeshlet.vertexCount = curUniqueVerts.size();
        curMeshlet.pos = curMeshletPos;
        curMeshlet.boundRadius = calculateBoundRadius(curBounds);
        meshlets.add(curMeshlet);

        return new MeshletGenOutput(meshlets, mesh, vertexIndices, localPrimitiveIndices);
    }

    public static Mesh mergeMeshes(List<Mesh> meshes) {
        var keys = meshes.get(0).vertAttributes.keySet();
        var newVertAttributes = new HashMap<Mesh.VERTATTRIB, List<Vector>>();
        var newIndices = new ArrayList<Integer>();

        for(var k: keys) {
            newVertAttributes.put(k, new ArrayList<>());
        }

        for(var mesh: meshes) {
            if(keys.equals(mesh.vertAttributes.keySet())) {
                int prevSize = newVertAttributes.get(Mesh.VERTATTRIB.POSITION).size();

                for(var k: keys) {
                    newVertAttributes.get(k).addAll(mesh.vertAttributes.get(k));
                }
                newIndices.addAll(mesh.indices.stream().map(i -> i + prevSize).toList());
            }
            else {
                throw new IllegalArgumentException("Vert attributes must be the same when merging meshes");
            }
        }

        return new Mesh(newIndices, null, newVertAttributes, null, meshes.get(0).meshLocation, null);
    }

    public enum MeshletColorMode {PerPrimitive, PerMeshlet}

    public static void setMeshletColors(MeshletColorMode colorMode, List<Meshlet> meshlets,
                                        Map<Mesh.VERTATTRIB, List<Vector>> globalVertAttribs,
                                        List<Integer> meshletVertexIndexBuffer,
                                        List<Integer> meshletLocalIndexBuffer, int vertsPerPrimitive) {

        var colorAttrib = new ArrayList<Vector>();
        var tempColorHashMap = new TreeMap<Integer, Vector>();
        var rand = new Random();

        if(colorMode == PerMeshlet) {

            for(var meshlet: meshlets) {
                Vector randomColor = Vector.getRandomVector(new Vector(0,0,0,1),
                        new Vector(1,1,1,1), rand);

                for(int i = meshlet.vertexBegin; i < meshlet.vertexBegin + meshlet.vertexCount; i++) {
                    var vertInd = meshletVertexIndexBuffer.get(i);
                    tempColorHashMap.put(vertInd, randomColor);
                }
            }
            // shouldn't be required

            for(int i = 0; i < globalVertAttribs.get(Mesh.VERTATTRIB.POSITION).size(); i++) {
                if(tempColorHashMap.containsKey(i)) {
                    colorAttrib.add(tempColorHashMap.get(i));
                }
                else {
                    colorAttrib.add(new Vector(1,1,1,1));
                }
            }
            globalVertAttribs.put(Mesh.VERTATTRIB.COLOR, colorAttrib);
        }

        else {
            for(int i = 0; i < globalVertAttribs.get(Mesh.VERTATTRIB.POSITION).size(); i++) {
                Vector randomColor = Vector.getRandomVector(new Vector(0,0,0,1),
                        new Vector(1,1,1,1), rand);
                colorAttrib.add(randomColor);
//                tempColorHashMap.put(i, randomColor);
            }
            globalVertAttribs.put(Mesh.VERTATTRIB.COLOR, colorAttrib);
//            globalVertAttribs.put(Mesh.VERTATTRIB.COLOR, new ArrayList<>(tempColorHashMap.values()));
        }
    }

    private static float calculateBoundRadius(BoundValues boundvalues) {
        float xRange = boundvalues.maxx - boundvalues.minx;
        float yRange = boundvalues.maxy - boundvalues.miny;
        float zRange = boundvalues.maxz - boundvalues.minz;

        float max1 = Math.max(xRange, yRange);
        float max2 = Math.max(max1, zRange);

        return max2/2.0f;
    }

    public static SortedVertAttribs sortMeshVertices(Map<Mesh.VERTATTRIB, List<Vector>> vertAttribs, List<Integer> indices) {

        var sorted = new ArrayList<PosWrapper>();
        var boundValues = new BoundValues();
        var verts = vertAttribs.get(Mesh.VERTATTRIB.POSITION);

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

        for(var key: vertAttribs.keySet()) {
            newAttribs.put(key, new ArrayList<>());
        }

        for(int i = 0; i < sorted.size(); i++) {
            var item = sorted.get(i);
            int index = item.prevIndex;
            indicesMapping.put(item.prevIndex, i);

            for(var key: vertAttribs.keySet()) {
                newAttribs.get(key).add(vertAttribs.get(key).get(index));
            }
        }

        var newIndices = new ArrayList<Integer>(indices.size());
        for(var prevInd: indices) {
            newIndices.add(indicesMapping.get(prevInd));
        }

        return new SortedVertAttribs(newAttribs, newIndices, boundValues);
    }

    public record PrimitiveWrapper(Vector pos, BoundValues boundValues, Integer... prevIndex) implements Comparable {
        @Override
        public int compareTo(Object o) {
            var v1 = (Vector) pos;
            var v2 = ((PrimitiveWrapper) o).pos;

            var morton1 = getMortonCode((long) (v1.get(0) - boundValues.minx), (long) (v1.get(1) - boundValues.miny), (long) (v1.get(2) - boundValues.minz));
            var morton2 = getMortonCode((long) (v2.get(0) - boundValues.minx), (long) (v2.get(1) - boundValues.miny), (long) (v2.get(2) - boundValues.minz));

            if (morton1 < morton2)
                return -1;
            else if (morton1 == morton2)
                return 0;
            else {
                return 1;
            }
        }
    }

    public static List<Integer> sortMeshIndices(List<Vector> verts, List<Integer> indices, int vertsPerPrimitive) {
        return sortMeshIndices(verts, indices, vertsPerPrimitive, null);
    }

        public static List<Integer> sortMeshIndices(List<Vector> verts, List<Integer> indices, int vertsPerPrimitive, BoundValues boundedValues) {

        var sorted = new ArrayList<PrimitiveWrapper>();
        var shouldCalcBounds = boundedValues == null? true: false;
        var boundValues = new BoundValues();

        if(!shouldCalcBounds)
            boundValues = boundedValues;

        for(int primIndex = 0; primIndex < indices.size()/vertsPerPrimitive; primIndex++) {

            int curBaseIndex = primIndex * vertsPerPrimitive;
            var primAveragePos = new Vector(0,0,0);
            var curIndices = new Integer[vertsPerPrimitive];

            for(int i = 0; i < vertsPerPrimitive; i++) {
                curIndices[i] = indices.get(curBaseIndex + i);
                var v = verts.get(curIndices[i]);
                primAveragePos = primAveragePos.add(v);

                if (shouldCalcBounds) {
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
            }
            primAveragePos = primAveragePos.scalarMul(1.0f/((float)vertsPerPrimitive));
            sorted.add(new PrimitiveWrapper(primAveragePos, boundValues, curIndices));
        }
        Collections.sort(sorted);

        var sortedIndices = new ArrayList<Integer>();
        for(var sortedPrim: sorted) {
            sortedIndices.addAll(Arrays.asList(sortedPrim.prevIndex));
        }

        return sortedIndices;
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
