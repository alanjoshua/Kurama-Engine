package engine.Math;

import engine.model.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DataAnalysis {

    public static List<Matrix> kMeanClustering(Matrix data, int expectedClusterCount) {

        List<Vector> dataPoints = data.convertToColumnVectorList();
        Vector[] bounds = Model.getBounds(dataPoints);
        Random random = new Random();
        random.setSeed(System.nanoTime());
        List<Vector> randPoints = new ArrayList<>();

        float dist = Math.abs(bounds[1].get(0) - bounds[0].get(0));
        float incr = dist / expectedClusterCount;

        for(int k = 0; k < expectedClusterCount;k++) {
            Vector minBound = new Vector(bounds[0]);
            minBound.setDataElement(0,bounds[0].get(0) + (k * incr));
            Vector maxBound = new Vector(bounds[1]);
            maxBound.setDataElement(0,bounds[0].get(0) + ((k+1) * incr));

            List<Vector> tempBounds = new ArrayList<>();
            tempBounds.add(minBound);
            tempBounds.add(maxBound);
            Vector avg = Vector.getAverage(tempBounds);

            Vector curr = Vector.getRandomVector(minBound,maxBound,random);
            randPoints.add(avg);
        }

//        System.out.println("initial centre values: ");
//        for(Vector v: randPoints) {
//            v.display();
//        }
//        System.out.println();

        boolean isDone = false;
        List<List<Vector>> clusters = null;

        while(!isDone) {
            clusters = new ArrayList<>();
            for(int count = 0; count < expectedClusterCount;count++) {
                clusters.add(new ArrayList<>());
            }

            for (Vector dataPoint : dataPoints) {
                int minIndex = 0;
                float minVal = Float.POSITIVE_INFINITY;
                for(int i = 0;i < randPoints.size();i++) {
                    float diff = randPoints.get(i).sub(dataPoint).getNorm();
                    if(diff < minVal) {
                        minVal = diff;
                        minIndex = i;
                    }
                }
                clusters.get(minIndex).add(dataPoint);
            }

            List<Vector> newRandPoints = new ArrayList<>();
            for(int i = 0;i < clusters.size();i++) {
                if(clusters.get(i).size() == 0) {
                    newRandPoints.add(randPoints.get(i));
                }
                else {
                    newRandPoints.add(Vector.getAverage(clusters.get(i)));
                }
            }

            isDone = true;
            for(int i = 0;i < randPoints.size();i++) {
                if(!randPoints.get(i).equals(newRandPoints.get(i))) {
                    isDone = false;
                    break;
                }
            }
            randPoints = newRandPoints;
        }

        List<Matrix> resClusters = new ArrayList<>();
        for(List<Vector> l: clusters) {
            resClusters.add(new Matrix(l));
        }

        return resClusters;

    }

    public static List<Matrix> HAC(Matrix data) {

        List<Matrix> resClusters = new ArrayList<>();
        List<Matrix> currClusters = new ArrayList<>();
        for(Vector v: data.convertToColumnVectorList()) {
            List<Vector> tempList = new ArrayList<>();
            tempList.add(v);
            currClusters.add(new Matrix(tempList));
        }

        while(currClusters.size() > 1) {
            Float[][] distMatrix = generateDistanceMatrix(currClusters);
            List<Integer> minClusters = getMinCluster(distMatrix);
            Matrix c1 = currClusters.get(minClusters.get(0));
            Matrix c2 = currClusters.get(minClusters.get(1));

//            c1.display();
//            System.out.println();
//            c2.display();
//            System.out.println("-----------------------------");

            if(!currClusters.remove(c1)) {
                throw new RuntimeException("Error while removing Matrix from current clusters");
            }
            if(!currClusters.remove(c2)) {
                throw new RuntimeException("Error while removing Matrix from current clusters");
            }

            List<Vector> combined = new ArrayList<>();
            for(Vector v: c1.convertToColumnVectorList()) {
                combined.add(v);
            }
            for(Vector v: c2.convertToColumnVectorList()) {
                combined.add(v);
            }
            Matrix combCluster = new Matrix(combined);
            resClusters.add(combCluster);
            currClusters.add(combCluster);
        }

        return resClusters;
    }

    private static List<Integer> getMinCluster(Float[][] distMatrix) {
        int minI = 0;
        int minJ = 0;
        float minVal = Float.POSITIVE_INFINITY;

        for(int i = 0;i < distMatrix.length;i++) {
            for(int j = 0;j < distMatrix[i].length;j++) {
                if(distMatrix[i][j] == null) {
                    break;
                }
                if(distMatrix[i][j] < minVal) {
                    minVal = distMatrix[i][j];
                    minI = i;
                    minJ = j;
                }
            }
        }

        List<Integer> res = new ArrayList<>();
        res.add(minI);
        res.add(minJ);
        return res;
    }

    private static Float[][] generateDistanceMatrix(List<Matrix> clusters) {
        Float[][] dist = new Float[clusters.size()][clusters.size()];
        for(int i = 0;i < clusters.size();i++) {
            for(int j = 0;j < clusters.size();j++) {
                if(i == j) {
                    dist[i][j] = null;
                    break;
                }
                else {
                    float minVal = Float.POSITIVE_INFINITY;
                    List<Vector> clus1 = clusters.get(i).convertToColumnVectorList();
                    List<Vector> clus2 = clusters.get(j).convertToColumnVectorList();
                    for(Vector a:clus1) {
                        for(Vector b:clus2) {
                            float tempDist = a.sub(b).getNorm();
                            if(tempDist < minVal) {
                                minVal = tempDist;
                            }
                        }
                    }
                    dist[i][j] = minVal;
                }
            }
        }

        return dist;
    }

}
