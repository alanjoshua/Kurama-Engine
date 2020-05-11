package engine.Math;

import engine.model.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

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

//    public static List<Matrix> performMatrixFactorisation(Matrix y, int loopCount, float learningRate) {
//        int userCount = y.getRows();
//        int movieCount = y.getCols();
//        int featureCount = Math.min(userCount,movieCount);
//
//        Matrix m1 = Matrix.createRandomMatrix(userCount,featureCount,null);
//        Matrix m2 = Matrix.createRandomMatrix(featureCount,movieCount,null);
//
//        for(int i = 0;i < loopCount;i++) {
//            Matrix yHat = m1.matMul(m2);
//            Matrix delEdelYHat = yHat.sub(y);
//            Matrix delEDelM1 = delEdelYHat.matMul(m2.transpose());
//            Matrix delEDelM2 = m1.transpose().matMul(delEdelYHat);
//            Matrix deltaM1 = delEDelM1.scalarMul(-learningRate);
//            Matrix deltaM2 = delEDelM2.scalarMul(-learningRate);
//            m1 = m1.add(deltaM1);
//            m2 = m2.add(deltaM2);
//        }
//
//        List<Matrix> res = new ArrayList<>();
//        res.add(m1);
//        res.add(m2);
//
//        return res;
//    }

    public static List<Matrix> NMTF(Matrix data, int loopCount, int k1, int k2) {
        Random random = new Random();
        random.setSeed(System.nanoTime());
        Supplier<Float> randomGen = () -> random.nextFloat();

        Matrix R = Matrix.createRandomMatrix(data.getRows(),k1,randomGen);
        Matrix C = Matrix.createRandomMatrix(k2, data.getCols(),randomGen);
        Matrix B = new Matrix(k1,k2,data.getAverage());

        for(int i = 0;i < loopCount;i++) {
//            Matrix tempRNum = data.matMul(C.transpose().matMul(B.transpose()));
//            Matrix tempRDenom = R.matMul(B.matMul(C.matMul(C.transpose().matMul(B.transpose()))));
//
//            Matrix tempBNum = R.transpose().matMul(data.matMul(C.transpose()));
//            Matrix tempBDenom = R.transpose().matMul(R.matMul(B.matMul(C.matMul(C.transpose()))));
//
//            Matrix tempCNum = B.transpose().matMul(R.transpose().matMul(data));
//            Matrix tempCDenom = B.transpose().matMul(R.transpose().matMul(R.matMul(B.matMul(C))));
//
////            tempRNum.display();
////            System.out.println();
//
//            float[][] newR = new float[R.getRows()][R.getCols()];
//            float[][] newB = new float[B.getRows()][B.getCols()];
//            float[][] newC = new float[C.getRows()][C.getCols()];
//
//            for(int r = 0; r < newR.length;r++) {
//                for(int c = 0; c < newR[r].length;c++) {
//                    newR[r][c] = R.get(r,c) * ((tempRNum.get(r,c) / tempRDenom.get(r,c)));
//                }
//            }
//
//            for(int r = 0; r < newB.length;r++) {
//                for(int c = 0; c < newB[r].length;c++) {
//                    newB[r][c] = B.get(r,c) * ((tempBNum.get(r,c) / tempBDenom.get(r,c)));
//                }
//            }
//
//            for(int r = 0; r < newC.length;r++) {
//                for(int c = 0; c < newC[r].length;c++) {
//                    newC[r][c] = C.get(r,c) * ((tempCNum.get(r,c) / tempCDenom.get(r,c)));
//                }
//            }
//
//            R = new Matrix(newR);
//            B = new Matrix(newB);
//            C = new Matrix(newC);

            R = R.mul((data.matMul(C.transpose().matMul(B.transpose()))).divide(R.matMul(B.matMul(C.matMul(C.transpose().matMul(B.transpose()))))));
            C = C.mul(((data.transpose().matMul(R.matMul(B))).divide(C.transpose().matMul(B.transpose().matMul(R.transpose().matMul(R.matMul(B)))))).transpose());
            B = B.mul((R.transpose().matMul(data.matMul(C.transpose()))).divide(R.transpose().matMul(R.matMul(B.matMul(C.matMul(C.transpose()))))));

           // R = R.orthogonalizeRows();
            B = B.orthogonalizeColumns();
            //C = C.orthogonalizeColumns();
        }

        List<Matrix> res = new ArrayList<>();
        res.add(R);
        res.add(B);
        res.add(C);
        return res;
    }

    //Non negative matrix factorisation
    public static List<Matrix> performMatrixFactorization(Matrix data, Integer ft, int loopCount, float learningRate, Float lowerBound, Float upperBound) {
        int userCount = data.getRows();
        int movieCount = data.getCols();
        int featureCount;
        if(ft == null) {
            featureCount = Math.min(userCount,movieCount);
        }
        else {
            featureCount = ft;
        }

        Random random = new Random();
        random.setSeed(System.nanoTime());
        Supplier<Float> randomGen = () -> random.nextFloat();

        Matrix m1 = Matrix.createRandomMatrix(userCount,featureCount,randomGen);
        Matrix m2 = Matrix.createRandomMatrix(featureCount,movieCount,randomGen);

//        Matrix previousDelta = null;
//        Matrix newM1 = null;
//        Matrix newM2 = null;
//        float learningRate = 1;

        for(int i = 0;i < loopCount;i++) {

            Matrix yHat = m1.matMul(m2);
            Matrix deltaY = yHat.sub(data);
            Matrix delEDelM1 = deltaY.matMul(m2.transpose());
            Matrix delEDelM2 = m1.transpose().matMul(deltaY);
            Matrix deltaM1 = delEDelM1.scalarMul(-learningRate);
            Matrix deltaM2 = delEDelM2.scalarMul(-learningRate);
            m1 = m1.add(deltaM1);
            m2 = m2.add(deltaM2);

            if(lowerBound != null || upperBound != null) {

                for (int r = 0; r < m1.getRows(); r++) {
                    for (int c = 0; c < m1.getCols(); c++) {
                        if (lowerBound != null) {
                            m1.getData()[r][c] = Math.max(m1.getData()[r][c], lowerBound);
                        }
                        if (upperBound != null) {
                            m1.getData()[r][c] = Math.min(m1.getData()[r][c], upperBound);
                        }
                        //m1.getData()[r][c] = m1.getData()[r][c]< lowerBound ? lowerBound: Math.min(m1.getData()[r][c], upperBound);
                    }
                }

                for (int r = 0; r < m2.getRows(); r++) {
                    for (int c = 0; c < m2.getCols(); c++) {
                        if (lowerBound != null) {
                            m2.getData()[r][c] = Math.max(m2.getData()[r][c], lowerBound);
                        }
                        if (upperBound != null) {
                            m2.getData()[r][c] = Math.min(m2.getData()[r][c], upperBound);
                        }
                        //m2.getData()[r][c] = m2.getData()[r][c]< lowerBound ? lowerBound: Math.min(m2.getData()[r][c], upperBound);
                    }
                }

            }

//            if(previousDelta == null) {
//                Matrix yHat = m1.matMul(m2);
//                previousDelta = yHat.sub(y);
//            }
//
//            Boolean shouldIncreaseLock = null;
//            boolean shouldStopLooping = false;
//            Matrix tempPrevM1 = null, tempPrevM2 = null;
//
//            while(!shouldStopLooping) {
//                Matrix delEDelM1 = previousDelta.matMul(m2.transpose());
//                Matrix delEDelM2 = m1.transpose().matMul(previousDelta);
//
//                Matrix deltaM1 = delEDelM1.scalarMul(-learningRate);
//                Matrix deltaM2 = delEDelM2.scalarMul(-learningRate);
//                newM1 = m1.add(deltaM1);
//                newM2 = m2.add(deltaM2);
//
//                for(int r = 0;r < newM1.getRows();r++) {
//                    for(int c = 0;c < newM1.getCols();c++) {
//                        newM1.getData()[r][c] = newM1.getData()[r][c]< lowerBound ? lowerBound: Math.min(newM1.getData()[r][c], upperBound);
//                    }
//                }
//
//                for(int r = 0;r < newM2.getRows();r++) {
//                    for(int c = 0;c < newM2.getCols();c++) {
//                        newM2.getData()[r][c] = newM2.getData()[r][c]< lowerBound ? lowerBound: Math.min(newM2.getData()[r][c], upperBound);
//                    }
//                }
//
//                if (shouldIncreaseLock == null) {
//                    shouldIncreaseLock = doesLearningRateSatisfy(m1, delEDelM1, m2, delEDelM2, previousDelta, newM1, newM2, sigma);
//                }
//                if(shouldIncreaseLock == true) {
//                    if(tempPrevM1!= null && tempPrevM2!= null) {
//                        if(tempPrevM1.sub(newM1).getNorm() == 0 || tempPrevM2.sub(newM2).getNorm() == 0) {
//                            shouldStopLooping = true;
//                            break;
//                        }
//                    }
//
//                    learningRate = learningRate/beta;
//                    if(doesLearningRateSatisfy(m1, delEDelM1, m2, delEDelM2, previousDelta, newM1, newM2, sigma)) {
//                        shouldStopLooping = false;
//                    }
//                    else {
//                        shouldStopLooping = true;
//                    }
//                }
//                else {
//                    learningRate = learningRate*beta;
//                    if(!doesLearningRateSatisfy(m1, delEDelM1, m2, delEDelM2, previousDelta, newM1, newM2, sigma)) {
//                        shouldStopLooping = false;
//                    }
//                    else {
//                        shouldStopLooping = true;
//                    }
//                }
//                tempPrevM1 = newM1;
//                tempPrevM2 = newM2;
//            }
//
//            m1 = newM1;
//            m2 = newM2;
//            System.out.println("outside");
//
        }

        List<Matrix> res = new ArrayList<>();
        res.add(m1);
        res.add(m2);

        return res;
    }

//    private static boolean doesLearningRateSatisfy(Matrix m1, Matrix deltaM1, Matrix m2, Matrix deltaM2, Matrix previousDelta, Matrix newM1, Matrix newM2, float sigma) {
//        Matrix newError = newM1.matMul(newM2);
//        float temporalError = newError.sub(previousDelta).getNorm();
//        float m1TemporalError = deltaM1.transpose().matMul(newM1.sub(m1)).scalarMul(sigma).getNorm();
//        float m2TemporalError = deltaM2.transpose().matMul(newM2.sub(m2)).scalarMul(sigma).getNorm();
//        if(temporalError <= m1TemporalError+m2TemporalError) {
//           // System.out.println(temporalError);
//            return true;
//        }
//        else {
//            return false;
//        }
//    }

}
