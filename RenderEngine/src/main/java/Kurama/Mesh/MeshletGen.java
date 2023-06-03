package Kurama.Mesh;

import Kurama.Math.Vector;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static Kurama.Mesh.MeshletGen.MeshletColorMode.*;
import static Kurama.utils.Logger.*;

public class MeshletGen {

    public record SortedVertAttribs(Map<Mesh.VERTATTRIB, List<Vector>> sortedVertAttribs, List<Integer> indices, BoundValues boundValues){}
    public static class BoundValues {
        public BoundValues(){}
        public void calculateRanges() {
            xRange = maxx - minx;
            yRange = maxy - miny;
            zRange = maxz - minz;
        }
        public Vector scalePoint(Vector p) {
            float newX, newY, newZ;

            newX = (p.get(0) - minx) / xRange;
            newY = (p.get(1) - miny) / yRange;
            newZ = (p.get(2) - minz) / zRange;

            return new Vector(newX, newY, newZ);
        }
        public float minx = Float.POSITIVE_INFINITY, maxx = Float.NEGATIVE_INFINITY,
                miny = Float.POSITIVE_INFINITY,maxy = Float.NEGATIVE_INFINITY,
                minz = Float.POSITIVE_INFINITY, maxz = Float.NEGATIVE_INFINITY,
        xRange, yRange, zRange;
    }

    public record MeshletGenOutput(List<Meshlet> meshlets, Mesh mesh, List<Integer> vertexIndexBuffer){}

    public record PosWrapper(Vector v, int prevIndex) implements Comparable {
        @Override
        public int compareTo(Object o) {

            var morton1 = getMortonCode(v);
            var morton2 = getMortonCode(((PosWrapper) o).v);

            if (morton1 < morton2)
                return -1;
            else if (morton1 == morton2)
                return 0;
            else {
                return 1;
            }
        }
    }

    public static MeshletGenOutput generateMeshlets(Mesh mesh, int maxVerts) {

        if(maxVerts <= 0) {
            throw new IllegalArgumentException("Max Vertices per meshlet cannot be " + maxVerts);
        }

        var sortedPoints = new ArrayList<PosWrapper>();
        var boundValues = new BoundValues();

        int ind = 0;
        for(var v: mesh.getVertices()) {
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

            sortedPoints.add(new PosWrapper(v, ind));
            ind++;
        }
        boundValues.calculateRanges();
        Collections.sort(sortedPoints);

        var meshlets = new ArrayList<Meshlet>();
        // Index list to resort the vertices before inserting into vertexbuffer
        var vertIndices = sortedPoints.stream().map(p -> p.prevIndex).collect(Collectors.toList());

        int numCompleteMeshlets = sortedPoints.size() / maxVerts;

        for(int i = 0; i < numCompleteMeshlets; i++) {
            var curMeshlet = new Meshlet();
            curMeshlet.vertexCount = maxVerts;
            curMeshlet.treeDepth = 0;
            curMeshlet.vertexBegin = (maxVerts*i);
            curMeshlet.pos = new Vector(0,0,0);

            boundValues = new BoundValues();
            for(var vertind: vertIndices.subList(curMeshlet.vertexBegin, curMeshlet.vertexBegin + maxVerts)) {
                var v = mesh.getVertices().get(vertind);
                curMeshlet.pos = curMeshlet.pos.add(v);
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

            boundValues.calculateRanges();
            curMeshlet.pos = curMeshlet.pos.scalarMul(1f/curMeshlet.vertexCount);
            curMeshlet.boundRadius = calculateBoundRadius(boundValues);
            curMeshlet.density = calculatePointCloudDensity(curMeshlet.boundRadius, curMeshlet.vertexCount);
            meshlets.add(curMeshlet);
        }

        // Last meshlet
        var curMeshlet = new Meshlet();
        curMeshlet.vertexCount = (vertIndices.size() - (maxVerts*numCompleteMeshlets));
        curMeshlet.treeDepth = 0;
        curMeshlet.vertexBegin = maxVerts*numCompleteMeshlets;
        curMeshlet.pos = new Vector(0,0,0);

        boundValues = new BoundValues();
        for(var vertind: vertIndices.subList(curMeshlet.vertexBegin, curMeshlet.vertexBegin + curMeshlet.vertexCount)) {
            var v = mesh.getVertices().get(vertind);
            curMeshlet.pos = curMeshlet.pos.add(v);
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
        boundValues.calculateRanges();
        curMeshlet.pos = curMeshlet.pos.scalarMul(1f/curMeshlet.vertexCount);
        curMeshlet.boundRadius = calculateBoundRadius(boundValues);
        curMeshlet.density = calculatePointCloudDensity(curMeshlet.boundRadius, curMeshlet.vertexCount);
        meshlets.add(curMeshlet);

        return new MeshletGenOutput(meshlets, mesh, vertIndices);
    }

    public static float calculatePointCloudDensity(float radius, int numPoints) {
        return (float) (numPoints/((4.0/3.0) * Math.PI * Math.pow(radius, 3)));
    }

    public static Meshlet genHierLODPointCloud(Mesh mesh, int maxVertsPerMeshlet, int maxChildrenPerLevel) {

//        var sortedPoints = new ArrayList<PosWrapper>(mesh.getVertices().size());
        var priorityQueue = new PriorityQueue<PosWrapper>(mesh.getVertices().size(),
                (p1, p2) -> {
                    var morton1 = getMortonCode(p1.v);
                    var morton2 = getMortonCode(p2.v);

                    if (morton1 < morton2)
                        return -1;
                    else if (morton1 == morton2)
                        return 0;
                    else {
                        return 1;
                    }
                });
        int ind = 0;
        log("Starting to insert vertices into priority queue");
        for(var v: mesh.getVertices()) {
//            sortedPoints.add(new PosWrapper(v, ind));
            priorityQueue.add(new PosWrapper(v, ind));
            ind++;
        }
//        boundValues.calculateRanges();
//        Collections.sort(sortedPoints);
        log("finished sorting");

        // Index list of the sorted points
//        var remainingVertIndices = sortedPoints.stream().map(p -> p.prevIndex).collect(Collectors.toList());
        var remainingVertIndices = priorityQueue.stream().map(p -> p.prevIndex).collect(Collectors.toList());

        var rootMeshlet = new Meshlet();
        rootMeshlet.parent = rootMeshlet;
        rootMeshlet.treeDepth = 0;

        // Recursively creates the hierarchy LOD structure, and all the info is stored in 'rootMeshlet'
        log("Generating the actual heirarchy structure here");
        return createHierarchyStructure(remainingVertIndices, rootMeshlet, maxChildrenPerLevel,
                maxVertsPerMeshlet, 0);
    }

    public static List<Meshlet> getMeshletsInBFOrder(Meshlet root) {

        List<Meshlet> meshlets = new ArrayList<>();
        var queue = new LinkedList<Meshlet>();
        queue.add(root);

        while(!queue.isEmpty()) {
            var cur = queue.remove();
            meshlets.add(cur);
            if(cur.children != null) {
                queue.addAll(cur.children);
            }
        }

        return meshlets;
    }

    public static int deepestLevel(Meshlet root, int currentDeepest) {
        if(root.treeDepth > currentDeepest) {
            currentDeepest = root.treeDepth;
        }

        for(var child: root.children) {
            var curDepth = deepestLevel(child, currentDeepest);
            if(curDepth > currentDeepest) {
                currentDeepest = curDepth;
            }
        }

        return currentDeepest;
    }

    public static int getNumMeshlets(Meshlet root) {
        if(root.children == null || root.children.size() == 0) {
            return 1;
        }

        else {
            int subtreeSize = 1;
            for(var child: root.children) {
                subtreeSize += getNumMeshlets(child);
            }
            return subtreeSize;
        }

    }

    public static int getNumVertsInHierarchy(Meshlet root) {
        if(root.children == null || root.children.size() == 0) {
            return root.vertIndices.size();
        }

        else {
            int subtreeSize = root.vertIndices.size();
            for(var child: root.children) {
                subtreeSize += getNumVertsInHierarchy(child);
            }
            return subtreeSize;
        }

    }

    // Recursively creates the hierarchy LOD structure, and all the info is stored in 'rootMeshlet'
    public static Meshlet createHierarchyStructure(List<Integer> sortedRemainingVertices, Meshlet rootMeshlet,
                                                   int maxNumChildren, int maxVertsPerChild, int curDepth) {

        // Reached leaf node, so just add all remaining vertices to curNode, and return
        if(sortedRemainingVertices.size() <= maxVertsPerChild) {
            rootMeshlet.treeDepth = curDepth;
            rootMeshlet.vertIndices = new ArrayList<>();
            rootMeshlet.vertIndices.addAll(sortedRemainingVertices);
        }

        else {

            // Randomly sample 'M' nodes from the remaining verts, and add it to the current meshlet
            List<Integer> randomSelection = IntStream.rangeClosed(0, sortedRemainingVertices.size()-1).boxed().collect(Collectors.toList());
            Collections.shuffle(randomSelection);
            rootMeshlet.treeDepth = curDepth;
            rootMeshlet.vertIndices = new ArrayList<>();
            rootMeshlet.children = new ArrayList<>();

            for(int i = 0; i < maxVertsPerChild; i++) {
                rootMeshlet.vertIndices.add(sortedRemainingVertices.get(randomSelection.get(i)));
            }

            // Now remove the vertices that were added to the current meshlet from the "sortedRemainingVertices" list
            sortedRemainingVertices.removeAll(rootMeshlet.vertIndices);

            int numChildrenPossible = sortedRemainingVertices.size() / maxVertsPerChild;

            // In case there are enough vertices available to make all children
            if(numChildrenPossible >= maxNumChildren) {
                int numVertsSubDivision = sortedRemainingVertices.size()/maxNumChildren;

                for(int i = 0; i < maxNumChildren; i++) {

                    var cMeshlet = new Meshlet();
                    cMeshlet.parent = rootMeshlet;
                    cMeshlet.treeDepth = curDepth + 1;
                    rootMeshlet.children.add(cMeshlet);
                    List<Integer> childVerts;

                    if(i < (maxNumChildren-1)) {
                        childVerts = new ArrayList<>(sortedRemainingVertices.subList(i * numVertsSubDivision, (i + 1) * numVertsSubDivision));
                    }
                    else {
                        childVerts = new ArrayList<>(sortedRemainingVertices.subList(i * numVertsSubDivision, sortedRemainingVertices.size()));
                    }

                    createHierarchyStructure(childVerts, cMeshlet, maxNumChildren, maxVertsPerChild, curDepth+1);
                }
            }

            // There are barely any vertices left, so the next level would just be leaf nodes
            else {
                for(int i = 0; i < maxNumChildren; i++) {

                    var cMeshlet = new Meshlet();
                    cMeshlet.parent = rootMeshlet;
                    cMeshlet.treeDepth = curDepth + 1;
                    rootMeshlet.children.add(cMeshlet);
                    List<Integer> childVerts;

                    int startPos = i * maxVertsPerChild;

                    // Last child
                    if ((sortedRemainingVertices.size() - startPos) < maxVertsPerChild) {
                        childVerts = new ArrayList<>(sortedRemainingVertices.subList(startPos, sortedRemainingVertices.size()));
                        createHierarchyStructure(childVerts, cMeshlet, maxNumChildren, maxVertsPerChild, curDepth+1);
                        break;
                    }

                    else {
                        childVerts = new ArrayList<>(sortedRemainingVertices.subList(startPos, startPos + maxVertsPerChild));
                        createHierarchyStructure(childVerts, cMeshlet, maxNumChildren, maxVertsPerChild, curDepth+1);
                    }
                }
            }

        }

        return rootMeshlet;
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

    public enum MeshletColorMode {PerPrimitive, PerMeshlet, PerHierarchyLevel}

    public static void setMeshletColors(MeshletColorMode colorMode, List<Meshlet> meshlets,
                                        Map<Mesh.VERTATTRIB, List<Vector>> globalVertAttribs) {

        var colorAttrib = new ArrayList<Vector>();
        var tempColorHashMap = new TreeMap<Integer, Vector>();
        var rand = new Random();

        var colorMap = new HashMap<Integer, Vector>();
        colorMap.put(0, new Vector(230f/255f, 25f/255f, 75/255f, 1f));
        colorMap.put(1, new Vector(60f/255f, 180f/255f, 75f/255f, 1f));
        colorMap.put(2, new Vector(255f/255f, 225f/255f, 25f/255f, 1f));
        colorMap.put(3, new Vector(0f/255f, 130f/255f, 200f/255f, 1f));
        colorMap.put(4, new Vector(245f/255f, 130f/255f, 48f/255f, 1f));
        colorMap.put(5, new Vector(145f/255f, 30f/255f, 180f/255f, 1f));
        colorMap.put(6, new Vector(70f/255f, 240f/255f, 240f/255f, 1f));
        colorMap.put(7, new Vector(240f/255f, 50f/255f, 230f/255f, 1f));
        colorMap.put(8, new Vector(210f/255f, 245f/255f, 60f/255f, 1f));

        var white = new Vector(1,1,1,1);
        if(colorMode == PerMeshlet) {
            int curColorInd = 0;
            for(var meshlet: meshlets) {
//                Vector randomColor = Vector.getRandomVector(new Vector(0,0,0,1),
//                        new Vector(1,1,1,1), rand);

//                var color = colorMap.get(curColorInd%8);
                var color = white;

                if(color == null) {
                    throw new RuntimeException("Gotten color cannot be null. key is: "+ curColorInd);
                }

                for(int i = meshlet.vertexBegin; i < meshlet.vertexBegin + meshlet.vertexCount; i++) {
                    tempColorHashMap.put(i, color);
                }
                curColorInd++;
            }
            // shouldn't be required
//            for(int i = 0; i < globalVertAttribs.get(Mesh.VERTATTRIB.POSITION).size(); i++) {
//                if(tempColorHashMap.containsKey(i)) {
//                    colorAttrib.add(tempColorHashMap.get(i));
//                }
//                else {
//                    colorAttrib.add(new Vector(1,1,1,1));
//                }
//            }
            globalVertAttribs.put(Mesh.VERTATTRIB.COLOR, tempColorHashMap.values().stream().toList());
        }

        else if (colorMode == PerPrimitive) {

            for(int i = 0; i < globalVertAttribs.get(Mesh.VERTATTRIB.POSITION).size(); i++) {
                Vector randomColor = Vector.getRandomVector(new Vector(0,0,0,1),
                        new Vector(1,1,1,1), rand);
                colorAttrib.add(randomColor);
            }
            globalVertAttribs.put(Mesh.VERTATTRIB.COLOR, colorAttrib);
        }

        else if(colorMode == PerHierarchyLevel) {

            var tally = new HashMap<Integer, Integer>();
            tally.put(-1, 0);
            tally.put(0, 0);
            tally.put(1, 0);
            tally.put(2, 0);
            tally.put(3, 0);
            tally.put(4, 0);
            tally.put(5, 0);
            tally.put(6, 0);
            tally.put(7, 0);
            tally.put(8, 0);
            tally.put(9, 0);

            for(var meshlet: meshlets) {
                tally.put(meshlet.treeDepth, tally.get(meshlet.treeDepth) + 1);
                for(int i = meshlet.vertexBegin; i < meshlet.vertexBegin + meshlet.vertexCount; i++) {
                    var vertInd = i;
                    tempColorHashMap.put(vertInd, colorMap.get(meshlet.treeDepth));
                }
            }
//            globalVertAttribs.put(Mesh.VERTATTRIB.COLOR, tempColorHashMap.values().stream().toList());
            for(int i = 0; i < globalVertAttribs.get(Mesh.VERTATTRIB.POSITION).size(); i++) {
                if(tempColorHashMap.containsKey(i) && tempColorHashMap.get(i) != null) {
                    colorAttrib.add(tempColorHashMap.get(i));
                }
                else {
                    colorAttrib.add(new Vector(1,1,1,1));
                }
            }
            globalVertAttribs.put(Mesh.VERTATTRIB.COLOR, colorAttrib);
            log(tally);
        }
    }

    public static float calculateBoundRadius(BoundValues boundvalues) {
        float xRange = boundvalues.maxx - boundvalues.minx;
        float yRange = boundvalues.maxy - boundvalues.miny;
        float zRange = boundvalues.maxz - boundvalues.minz;

        float max1 = Math.max(xRange, yRange);
        float max2 = Math.max(max1, zRange);

        return max2/2.0f;
    }

    public static SortedVertAttribs sortMeshVertices(Map<Mesh.VERTATTRIB, List<Vector>> vertAttribs, List<Integer> indices) {

        var sorted = new LinkedList<PosWrapper>();
        var boundValues = new BoundValues();
        var verts = vertAttribs.get(Mesh.VERTATTRIB.POSITION);

        // Find range of values to offset and make everything positive
        for(int i = 0; i < verts.size(); i++) {
            var v = verts.get(i);
            sorted.add(new PosWrapper(v, i));

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
            var v1 = boundValues.scalePoint(pos);
            var v2 = boundValues.scalePoint(((PrimitiveWrapper) o).pos);

            var morton1 = getMortonCode(v1);
            var morton2 = getMortonCode(v2);

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

        var sorted = new LinkedList<PrimitiveWrapper>();
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
            boundValues.calculateRanges();
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

    public static long getMortonCode(Vector v) {
        Long x = (long)v.get(0);
        Long y = (long)v.get(1);
        Long z = (long)v.get(2);
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
