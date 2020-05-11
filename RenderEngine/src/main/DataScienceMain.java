package main;

import engine.Math.DataAnalysis;
import engine.Math.Matrix;

import java.util.List;

public class DataScienceMain {

    public static void main(String[] args) {

        float[][] yData = {
                {3,1,1,3,1},
                {1,2,4,1,3},
                {3,1,1,3,1},
                {4,3,5,4,4}};
        Matrix y = new Matrix(yData);

//        List<Matrix> trif = DataAnalysis.NMTF(y,1000,2,2);
//        Matrix B = trif.get(1);
//        Matrix R = trif.get(0);
//        Matrix C = trif.get(2);
//
//        System.out.println("Row Matrix (multiplied with block matrix)");
//        R.matMul(B).convertMatrixToBinaryByRow().display();
//        System.out.println("\nBlock structure matrix");
//        B.display();
//        System.out.println();
//        System.out.println("Column matrix (multiplied with block matrix)");
//        B.matMul(C).convertMatrixToBinaryByColumn().display();
//        System.out.println();
//        R.matMul(B.matMul(C)).display();

        List<Matrix> res = DataAnalysis.performMatrixFactorization(y,2,10000,0.01f,0f,null);

        Matrix m1 = res.get(0);
        Matrix m2 = res.get(1);
        System.out.println("M1");
        m1.convertMatrixToBinaryByRow().display();
        System.out.println("M2");
        m2.convertMatrixToBinaryByColumn().display();
        System.out.println("yHat");
        m1.matMul(m2).display();

//        float[][] data =   {{20,1,2,3,6,10,11,12},
//                            {20,1,2,3,6,10,11,12},
//                            {20,1,2,3,6,10,11,12}};
//
//        List<Matrix> kMeanClusters = DataAnalysis.kMeanClustering(new Matrix(data),2);
//        System.out.println("K-Mean cluster:");
//        for(int i = 0;i < kMeanClusters.size();i++) {
//            System.out.println("cluster "+(i+1) + ": ");
//            kMeanClusters.get(i).display();
//            System.out.println("------------------------------");
//        }

//        List<Matrix> hacClusters =  DataAnalysis.HAC(new Matrix(data));
//        System.out.println("HAC:");
//        for(int i = 0;i < hacClusters.size();i++) {
//            System.out.println("cluster "+(i+1) + ": ");
//            hacClusters.get(i).display();
//            System.out.println("------------------------------");
//        }
    }

}
