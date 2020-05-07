package main;

import engine.Math.DataAnalysis;
import engine.Math.Matrix;

import java.util.List;

public class DataScienceMain {

    public static void main(String[] args) {
        float[][] data =   {{20,1,2,3,6,10,11,12},
                            {20,1,2,3,6,10,11,12},
                            {20,1,2,3,6,10,11,12}};

        List<Matrix> kMeanClusters = DataAnalysis.kMeanClustering(new Matrix(data),2);

//        System.out.println("K-Mean cluster:");
//        for(int i = 0;i < kMeanClusters.size();i++) {
//            System.out.println("cluster "+(i+1) + ": ");
//            kMeanClusters.get(i).display();
//            System.out.println("------------------------------");
//        }

        List<Matrix> hacClusters =  DataAnalysis.HAC(new Matrix(data));

        System.out.println("HAC:");
        for(int i = 0;i < hacClusters.size();i++) {
            System.out.println("cluster "+(i+1) + ": ");
            hacClusters.get(i).display();
            System.out.println("------------------------------");
        }

    }

}
